package com.ttclub.backend.service;

import com.ttclub.backend.model.AuthProvider;
import com.ttclub.backend.model.MfaRecoveryCode;
import com.ttclub.backend.model.PreAuthPurpose;
import com.ttclub.backend.model.PreAuthSession;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.MfaRecoveryCodeRepository;
import com.ttclub.backend.repository.PreAuthSessionRepository;
import com.ttclub.backend.repository.UserRepository;
import com.ttclub.backend.security.SecretCrypto;
import com.ttclub.backend.security.TokenHashingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * TOTP (RFC 6238) utility + MFA orchestration.
 * - Base32 uppercase secrets (A-Z, 2-7), no padding
 * - HMAC-SHA1, 30s step, 6 digits, skew +-1
 * - Secrets stored encrypted (AES-GCM via SecretCrypto)
 * - Recovery codes stored hashed (SHA-256)
 */
@Service
@Transactional
public class MfaService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;

    private final UserRepository users;
    private final PreAuthSessionRepository preAuth;
    private final MfaRecoveryCodeRepository recovery;
    private final PasswordEncoder pe;
    private final SecretCrypto crypto;
    private final TokenHashingService hasher;
    private final String issuer;
    private final int mfaVerifyMaxAttempts;

    public MfaService(UserRepository users,
                      PreAuthSessionRepository preAuth,
                      MfaRecoveryCodeRepository recovery,
                      PasswordEncoder pe,
                      SecretCrypto crypto,
                      TokenHashingService hasher,
                      @Value("${ttclub.mfa.issuer:TT Club}") String issuer,
                      @Value("${ttclub.mfa.verify-max-attempts:6}") int maxAttempts) {
        this.users = users;
        this.preAuth = preAuth;
        this.recovery = recovery;
        this.pe = pe;
        this.crypto = crypto;
        this.hasher = hasher;
        this.issuer = issuer;
        this.mfaVerifyMaxAttempts = Math.max(1, maxAttempts);
    }

    /* Setup/Enable/Disable for logged-in user */

    public MfaSetupResult setup(User user, String password) {
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("OAuth-managed accounts cannot change password.");
        }
        if (!pe.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect password.");
        }
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new IllegalStateException("MFA already enabled");
        }

        String secret = newSecret();
        user.setMfaSecretTmpEnc(crypto.encrypt(secret));
        users.save(user);

        String label = issuer + ":" + user.getEmail();
        String uri = "otpauth://totp/" +
                url(label) +
                "?secret=" + secret +
                "&issuer=" + url(issuer) +
                "&period=" + STEP_SECONDS +
                "&digits=" + DIGITS +
                "&algorithm=SHA1";

        // QR data URL: keep optional - frontend can generate QR from otpauth URL if needed
        String qrDataUrl = null;
        try {
            qrDataUrl = QrPng.dataUrl(uri, 240);
        } catch (Throwable ignore) {
            // ZXing not on classpath or graphics unavailable -> skip QR (frontend will render)
        }

        return new MfaSetupResult(uri, maskSecret(secret), qrDataUrl);
    }

    public List<String> enable(User user, String code) {
        if (user.getMfaSecretTmpEnc() == null || user.getMfaSecretTmpEnc().isBlank()) {
            throw new IllegalStateException("MFA setup required");
        }
        String secret = crypto.decrypt(user.getMfaSecretTmpEnc());
        if (!verifyTotp(secret, code, System.currentTimeMillis())) {
            throw new SecurityException("Invalid MFA code");
        }

        user.setMfaSecretEnc(crypto.encrypt(secret));
        user.setMfaSecretTmpEnc(null);
        user.setMfaEnabled(true);
        users.save(user);

        // Generate and store recovery codes (8 random codes)
        recovery.deleteByUser(user);
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String raw = randomRecoveryCode();
            MfaRecoveryCode row = new MfaRecoveryCode();
            row.setUser(user);
            row.setCodeHash(hasher.hash(raw));
            recovery.save(row);
            codes.add(raw);
        }
        return codes;
    }

    public void disable(User user, String password, String recoveryCode) {
        boolean ok = false;
        if (password != null && !password.isBlank()) {
            if (user.getProvider() == AuthProvider.LOCAL && pe.matches(password, user.getPasswordHash())) {
                ok = true;
            }
        }
        if (!ok && recoveryCode != null && !recoveryCode.isBlank()) {
            var found = recovery.findByUserAndCodeHash(user, hasher.hash(recoveryCode))
                    .orElse(null);
            if (found != null && found.getUsedAt() == null) {
                found.setUsedAt(Instant.now());
                recovery.save(found);
                ok = true;
            }
        }
        if (!ok) {
            throw new SecurityException("Invalid password or recovery code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecretEnc(null);
        user.setMfaSecretTmpEnc(null);
        users.save(user);
        recovery.deleteByUser(user);
    }

    public void adminDisable(long userId) {
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setMfaEnabled(false);
        u.setMfaSecretEnc(null);
        u.setMfaSecretTmpEnc(null);
        users.save(u);
        recovery.deleteByUser(u);
    }

    /* Login-time second factor */

    /**
     * Create a pre-auth token with an explicit pwdChangeRequired flag to be propagated
     * through the MFA step. Useful when the first factor used a temporary password.
     */
    public String createPreAuthToken(User user, long ttlSeconds, boolean pwdChangeRequired) {
        String raw = UUID.randomUUID().toString();
        String hash = hasher.hash(raw);
        PreAuthSession row = new PreAuthSession();
        row.setTokenHash(hash);
        row.setUser(user);
        row.setPurpose(PreAuthPurpose.LOGIN);
        row.setAttempts(0);
        row.setPwdChangeRequired(pwdChangeRequired);
        row.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        preAuth.save(row);
        return raw;
    }

    /** Backward-compatible overload that defaults pwdChangeRequired to false. */
    public String createPreAuthToken(User user, long ttlSeconds) {
        return createPreAuthToken(user, ttlSeconds, false);
    }

    /**
     * Backward-compatible method retained for existing callers.
     * Delegates to the detailed variant and returns only the User.
     */
    public User verifyPreAuthAndCode(String rawToken, String code) {
        MfaVerifyResult r = verifyPreAuthAndCodeDetailed(rawToken, code);
        return r.user();
    }

    /**
     * Detailed variant that returns both the authenticated user and whether a
     * password change is required (as carried on the pre-auth session).
     */
    public MfaVerifyResult verifyPreAuthAndCodeDetailed(String rawToken, String code) {
        String hash = hasher.hash(rawToken);
        PreAuthSession row = preAuth.findByTokenHash(hash).orElseThrow(() ->
                new SecurityException("MFA token invalid"));

        if (row.getExpiresAt().isBefore(Instant.now())) {
            preAuth.delete(row);
            throw new SecurityException("MFA token invalid");
        }
        if (row.getAttempts() >= mfaVerifyMaxAttempts) {
            preAuth.delete(row);
            throw new SecurityException("Too many attempts");
        }

        User u = row.getUser();
        String enc = u.getMfaSecretEnc();
        if (!Boolean.TRUE.equals(u.getMfaEnabled()) || enc == null || enc.isBlank()) {
            preAuth.delete(row);
            throw new IllegalStateException("MFA not enabled");
        }

        String secret = crypto.decrypt(enc);
        boolean ok = verifyTotp(secret, code, System.currentTimeMillis());
        if (!ok) {
            row.setAttempts(row.getAttempts() + 1);
            preAuth.save(row);
            throw new SecurityException("Invalid MFA code");
        }

        boolean pwdChangeRequired = row.isPwdChangeRequired();
        preAuth.delete(row);
        return new MfaVerifyResult(u, pwdChangeRequired);
    }

    /* helpers  */

    private static String newSecret() {
        // 20 random bytes -> 32-char Base32 (no padding)
        byte[] b = new byte[20];
        RNG.nextBytes(b);
        return base32Enc(b);
    }

    private static String maskSecret(String s) {
        String clean = s.replaceAll("\\s+", "");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            if (i > 0 && i % 4 == 0) out.append(' ');
            out.append(clean.charAt(i));
        }
        return out.toString();
    }

    private static String randomRecoveryCode() {
        // 10 Base32 chars grouped 5-5 -> "ABCDE-FGHIJ"
        byte[] b = new byte[6];
        RNG.nextBytes(b);
        String raw = base32Enc(b).substring(0, 10);
        return raw.substring(0, 5) + "-" + raw.substring(5);
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static boolean verifyTotp(String base32Secret, String code, long nowMs) {
        if (code == null || !code.matches("\\d{6}")) return false;
        long step = (nowMs / 1000L) / STEP_SECONDS;
        try {
            byte[] key = base32Dec(base32Secret);
            for (long off = -1; off <= 1; off++) {
                int exp = hotp(key, step + off, DIGITS);
                String expStr = String.format(Locale.ROOT, "%06d", exp);
                if (constantTimeEquals(expStr, code)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static int hotp(byte[] key, long counter, int digits) throws GeneralSecurityException {
        byte[] cnt = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] h = mac.doFinal(cnt);
        int off = h[h.length - 1] & 0x0F;
        int bin = ((h[off] & 0x7F) << 24) | ((h[off + 1] & 0xFF) << 16) | ((h[off + 2] & 0xFF) << 8) | (h[off + 3] & 0xFF);
        int mod = (int) Math.pow(10, digits);
        return bin % mod;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= (a.charAt(i) ^ b.charAt(i));
        return r == 0;
    }

    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static String base32Enc(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int idx = (buffer >> (bitsLeft - 5)) & 31;
                bitsLeft -= 5;
                out.append(B32.charAt(idx));
            }
        }
        if (bitsLeft > 0) {
            int idx = (buffer << (5 - bitsLeft)) & 31;
            out.append(B32.charAt(idx));
        }
        return out.toString();
    }

    private static byte[] base32Dec(String s) {
        String clean = s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z2-7]", "");
        int buffer = 0, bitsLeft = 0;
        ArrayList<Byte> out = new ArrayList<>();
        for (int i = 0; i < clean.length(); i++) {
            int val = B32.indexOf(clean.charAt(i));
            if (val < 0) throw new IllegalArgumentException("Bad base32");
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.add((byte) ((buffer >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }
        byte[] res = new byte[out.size()];
        for (int i = 0; i < out.size(); i++) res[i] = out.get(i);
        return res;
    }

    /* optional QR using ZXing if on classpath */
    private static class QrPng {
        static String dataUrl(String text, int size) throws Exception {
            // If ZXing is not available at runtime, this class throws; caller will ignore and let frontend render QR
            com.google.zxing.qrcode.QRCodeWriter w = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix m = w.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, size, size);
            int width = m.getWidth();
            int height = m.getHeight();
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            int on = 0x000000, off = 0xFFFFFF;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    img.setRGB(x, y, m.get(x, y) ? on : off);
                }
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + b64;
        }
    }

    /** DTO (service-level) for setup step. */
    public static record MfaSetupResult(String otpauthUrl, String maskedSecret, String qrDataUrl) { }

    /** DTO returned by detailed verify. */
    public static record MfaVerifyResult(User user, boolean pwdChangeRequired) { }
}
