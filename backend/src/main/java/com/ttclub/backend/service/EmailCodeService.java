package com.ttclub.backend.service;

import com.ttclub.backend.model.EmailVerificationCode;
import com.ttclub.backend.repository.EmailVerificationCodeRepository;
import com.ttclub.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

/**
 * Generates, stores, validates and (optionally) sends 6-digit verification codes.
 * Rules:
 *  - Code is 6 digits, TTL is configurable (default 10 minutes), single-use.<br>
 *  - If email exists (LOCAL or OAUTH), send-code returns 409 EMAIL_EXISTS.<br>
 *  - When ttclub.mail.enabled=false, no email is sent; content is logged instead.
 */
@Service
@Transactional
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final EmailVerificationCodeRepository codes;
    private final UserRepository users;
    private final JavaMailSender mail;

    private final boolean mailEnabled;
    private final int ttlSeconds;

    public EmailCodeService(EmailVerificationCodeRepository codes,
                            UserRepository users,
                            JavaMailSender mailSender,
                            @Value("${ttclub.mail.enabled:true}") boolean mailEnabled,
                            @Value("${ttclub.auth.code-ttl-sec:600}") int ttlSeconds) {
        this.codes = codes;
        this.users = users;
        this.mail = mailSender;
        this.mailEnabled = mailEnabled;
        this.ttlSeconds = ttlSeconds;
    }

    /** Generate and send a code unless a user already exists for that email. */
    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        // strict duplicate prevention: do not send if account exists (LOCAL or OAUTH)
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        String code = String.format(Locale.ROOT, "%06d", RNG.nextInt(1_000_000));
        Instant exp = Instant.now().plusSeconds(ttlSeconds);

        codes.findByEmailIgnoreCase(email).ifPresentOrElse(row -> {
            row.setCode(code);
            row.setExpiresAt(exp);
        }, () -> codes.save(new EmailVerificationCode(email, code, exp)));

        if (!mailEnabled) {
            log.info("[DEV-MAIL] verification code for {} is {} (expires in {} seconds)", email, code, ttlSeconds);
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Your verification code");
        msg.setText(buildBody(code, ttlSeconds));
        mail.send(msg);
    }

    /** Validate and consume the code (single-use). Throws on invalid or expired. */
    public void assertValid(String rawEmail, String providedCode) {
        String email = normalize(rawEmail);

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (providedCode == null || !providedCode.matches("\\d{6}")) {
            // do not leak specifics
            throw new IllegalArgumentException("Invalid verification code");
        }

        EmailVerificationCode row = codes.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code"));

        if (row.getExpiresAt().isBefore(Instant.now())) {
            codes.delete(row);
            throw new IllegalArgumentException("Invalid verification code");
        }
        if (!row.getCode().equals(providedCode)) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        // single-use
        codes.delete(row);
    }

    private static String normalize(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String buildBody(String code, int ttl) {
        int mins = Math.max(1, ttl / 60);
        return "Your TT Club verification code is: " + code + "\n\n" +
                "This code expires in " + mins + " minutes. If you did not request this, please ignore this email and contact the Club in Ottawa Ontario.";
    }
}
