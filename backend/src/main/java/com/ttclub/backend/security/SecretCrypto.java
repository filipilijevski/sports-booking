package com.ttclub.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecretCrypto {

    private static final SecureRandom RNG = new SecureRandom();
    private final byte[] key; // 32 bytes (SHA-256 of configured key)

    public SecretCrypto(@Value("${ttclub.mfa.enc-key:${jwt.secret}}") String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("Missing ttclub.mfa.enc-key (may fallback to jwt.secret)");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            this.key = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[12];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec ks = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, ks, new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer bb = ByteBuffer.allocate(4 + iv.length + ct.length);
            bb.putInt(iv.length).put(iv).put(ct);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt failed", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] blob = Base64.getUrlDecoder().decode(encoded);
            ByteBuffer bb = ByteBuffer.wrap(blob);
            int ivLen = bb.getInt();
            byte[] iv = new byte[ivLen];
            bb.get(iv);
            byte[] ct = new byte[bb.remaining()];
            bb.get(ct);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec ks = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, ks, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decrypt failed", e);
        }
    }
}
