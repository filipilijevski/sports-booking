package com.ttclub.backend.service;

import com.ttclub.backend.config.JwtUtil;
import com.ttclub.backend.config.RegistrationProperties;
import com.ttclub.backend.dto.*;
import com.ttclub.backend.mapper.UserMapper;
import com.ttclub.backend.model.*;
import com.ttclub.backend.repository.*;
import com.ttclub.backend.security.TokenHashingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class AuthService {

    private final UserRepository         users;
    private final RoleRepository         roles;
    private final RefreshTokenRepository refreshTokens;
    private final RegistrationProperties regProps;
    private final PasswordEncoder        pe;
    private final JwtUtil                jwt;
    private final UserMapper             mapper;
    private final EmailCodeService       emailCodes;
    private final TokenHashingService    hasher;
    private final MfaService             mfa;
    private final long                   refreshTtlSec;
    private final JavaMailSender         mail;
    private final boolean                mailEnabled;

    private static final SecureRandom RNG = new SecureRandom();

    public AuthService(UserRepository users,
                       RoleRepository roles,
                       RefreshTokenRepository refreshTokens,
                       RegistrationProperties regProps,
                       PasswordEncoder pe,
                       JwtUtil jwt,
                       UserMapper mapper,
                       EmailCodeService emailCodes,
                       TokenHashingService hasher,
                       MfaService mfa,
                       JavaMailSender mailSender,
                       @Value("${ttclub.auth.refresh-ttl-sec:43200}") long refreshTtlSec,
                       @Value("${ttclub.mail.enabled:true}") boolean mailEnabled) {
        this.users          = users;
        this.roles          = roles;
        this.refreshTokens  = refreshTokens;
        this.regProps       = regProps;
        this.pe             = pe;
        this.jwt            = jwt;
        this.mapper         = mapper;
        this.emailCodes     = emailCodes;
        this.hasher         = hasher;
        this.mfa            = mfa;
        this.mail           = mailSender;
        this.refreshTtlSec  = refreshTtlSec;
        this.mailEnabled    = mailEnabled;
    }

    public void sendVerificationCode(String emailRaw) {
        emailCodes.sendCode(emailRaw);
    }

    public LoginResponseDto register(RegisterDto dto) {
        Objects.requireNonNull(dto.verificationCode(), "verificationCode is required");

        String email = normalize(dto.email());
        if (!StringUtils.hasText(email)) throw new IllegalArgumentException("Email cannot be blank");
        if (users.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("Email already exists");

        emailCodes.assertValid(email, dto.verificationCode());
        RoleName targetRole = resolveTargetRole(dto);

        User entity = mapper.toEntity(dto);
        entity.setEmail(email);
        entity.setPasswordHash(pe.encode(dto.password()));
        entity.setFirstName(dto.firstName() == null ? null : dto.firstName().trim());
        entity.setLastName(dto.lastName() == null ? null : dto.lastName().trim());
        entity.setRole(
                roles.findByName(targetRole)
                        .orElseThrow(() -> new IllegalStateException("Role row missing: " + targetRole))
        );
        users.save(entity);

        return issueTokens(entity);
    }

    /**
     * Login: accepts either the real password OR, if present and not expired, the temporary password.
     * When temp password is used, JWT carries claim pwd_change_required=true and API is gated.
     */
    public Object login(LoginDto dto) {
        String email = normalize(dto.email());

        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Bad credentials"));

        if (user.getDeletedAt() != null) {
            throw new IllegalArgumentException("Bad credentials");
        }

        boolean realOk = pe.matches(dto.password(), user.getPasswordHash());
        boolean tempOk = false;

        if (!realOk && user.getTempPasswordHash() != null && user.getTempPasswordExpiresAt() != null) {
            if (user.getTempPasswordExpiresAt().isAfter(Instant.now())) {
                tempOk = pe.matches(dto.password(), user.getTempPasswordHash());
            }
        }

        if (!realOk && !tempOk) throw new IllegalArgumentException("Bad credentials");

        boolean mustChange = tempOk;

        if (Boolean.TRUE.equals(user.getMfaEnabled()) && user.getMfaSecretEnc() != null && !user.getMfaSecretEnc().isBlank()) {
            String token = mfa.createPreAuthToken(user, 300, mustChange);
            return new com.ttclub.backend.dto.MfaDtos.MfaChallengeDto("MFA_REQUIRED", token, List.of("TOTP"));
        }

        return mustChange
                ? issueTokens(user, Map.of("pwd_change_required", true))
                : issueTokens(user);
    }

    public LoginResponseDto refresh(String oldRefreshToken) {
        String hash = hasher.hash(oldRefreshToken);
        RefreshToken row = refreshTokens.findByToken(hash)
                .orElseGet(() -> refreshTokens.findByToken(oldRefreshToken)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid refreshToken")));

        if (row.getExpiresAt().isBefore(Instant.now())) {
            refreshTokens.delete(row);
            throw new IllegalArgumentException("Refresh token expired");
        }
        User user = row.getUser();

        refreshTokens.delete(row);
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        String hash = hasher.hash(refreshToken);
        refreshTokens.deleteByToken(hash);
        refreshTokens.deleteByToken(refreshToken);
    }

    /* Password reset: email-only, LOCAL accounts */
    public void requestPasswordReset(String rawEmail) {
        String email = normalize(rawEmail);
        if (!StringUtils.hasText(email)) return;   // noop

        User u = users.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) return; // avoid enumeration

        if (u.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("OAuth managed account");
        }

        String temp = generateTempPassword(20);
        u.setTempPasswordHash(pe.encode(temp));
        u.setTempPasswordExpiresAt(Instant.now().plusSeconds(45 * 60));
        users.save(u);

        // send/log email with the temporary password
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Your temporary password");
        msg.setText("""
                A temporary password has been generated for your TT Club account.

                Temporary password: %s
                It will expire in 45 minutes.

                Sign in with the temporary password, and you will be required to set a new password before continuing.
                If you did not request this change, you can ignore this email.
                """.formatted(temp));
        if (mailEnabled) {
            mail.send(msg);
        } else {
            // Email is logged by Logging sender (GmailOAuth2Config) as well; this is a second explicit log.
            org.slf4j.LoggerFactory.getLogger(AuthService.class).info("[DEV-MAIL] temp password for {} is {}", email, temp);
        }
    }

    /* Helpers */

    public LoginResponseDto issueTokensForUser(User user) {
        return issueTokens(user);
    }
    public LoginResponseDto issueTokensForUser(User user, Map<String,Object> claims) {
        return issueTokens(user, claims);
    }

    private LoginResponseDto issueTokens(User user) { return issueTokens(user, Map.of()); }

    private LoginResponseDto issueTokens(User user, Map<String, Object> claims) {
        String access     = jwt.generate(user, claims);
        String refreshRaw = UUID.randomUUID().toString();
        String refreshHashed = hasher.hash(refreshRaw);

        refreshTokens.save(new RefreshToken(
                refreshHashed,
                user,
                Instant.now().plusSeconds(refreshTtlSec)));

        return new LoginResponseDto(access, refreshRaw, user.getFirstName());
    }

    private RoleName resolveTargetRole(RegisterDto dto) {
        Map<RoleName, String> codes = regProps.getCodes();
        if (dto.requestedRole() != null && dto.requestedRole() != RoleName.CLIENT) {
            String required = codes.get(dto.requestedRole());
            if (!Objects.equals(required, dto.registrationCode()))
                throw new IllegalArgumentException("Invalid registration code for role " + dto.requestedRole());
            return dto.requestedRole();
        }
        if (dto.registrationCode() != null && !dto.registrationCode().isBlank()) {
            return codes.entrySet().stream()
                    .filter(e -> e.getValue().equals(dto.registrationCode()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(RoleName.CLIENT);
        }
        return RoleName.CLIENT;
    }

    private static String normalize(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String generateTempPassword(int len) {
        final char[] alphabet = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray(); // no 0/O/I/l/1
        char[] out = new char[len];
        for (int i = 0; i < len; i++) out[i] = alphabet[RNG.nextInt(alphabet.length)];
        return new String(out);
    }
}
