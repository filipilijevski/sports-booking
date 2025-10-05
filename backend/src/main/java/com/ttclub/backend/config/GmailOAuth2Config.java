package com.ttclub.backend.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Arrays;
import java.util.Properties;

@Configuration(proxyBeanMethods = false)
public class GmailOAuth2Config {

    private static final Logger log = LoggerFactory.getLogger(GmailOAuth2Config.class);

    /**
     * Opt‑in XOAUTH2 sender. Enable with:
     *   gmail.oauth2.enabled=true
     * and provide gmail.user / client-id / client-secret / refresh-token.
     * If disabled, Spring Boot's spring.mail.* auto-config can supply a JavaMailSender
     * (e.g. Gmail App Password). If none is present, the loggingSender() below is used.
     */
    @Bean
    @ConditionalOnProperty(name = "gmail.oauth2.enabled", havingValue = "true", matchIfMissing = false)
    JavaMailSender gmailOAuth2Sender(
            @Value("${gmail.client-id:}")     String clientId,
            @Value("${gmail.client-secret:}") String clientSecret,
            @Value("${gmail.refresh-token:}") String refreshToken,
            @Value("${gmail.user:}")          String gmailUser
    ) {
        boolean hasAll =
                !clientId.isBlank() &&
                        !clientSecret.isBlank() &&
                        !refreshToken.isBlank() &&
                        !gmailUser.isBlank();

        if (!hasAll) {
            throw new IllegalStateException("gmail.oauth2.enabled=true but one or more gmail.* properties are missing");
        }

        UserCredentials creds = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        Authenticator auth = new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                try {
                    AccessToken at = creds.refreshAccessToken();
                    return new PasswordAuthentication(gmailUser, at.getTokenValue());
                } catch (Exception ex) {
                    throw new RuntimeException("Unable to refresh Gmail token", ex);
                }
            }
        };

        Properties p = new Properties();
        p.put("mail.smtp.auth",            "true");
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.auth.mechanisms", "XOAUTH2");

        Session session = Session.getInstance(p, auth);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setSession(session);
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(gmailUser);   // “From:”
        sender.setProtocol("smtp");

        log.info("Gmail XOAUTH2 mail-sender initialised.");
        return sender;
    }

    /**
     * Dev-safe logging sender used when no JavaMailSender bean exists
     * (i.e. neither spring.mail.* nor gmail.oauth2.enabled are configured).
     */
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    JavaMailSender loggingSender() {
        log.warn("No mail sender configured - e-mails will be logged only.");
        return new JavaMailSenderImpl() {
            @Override public void send(SimpleMailMessage msg) throws MailException {
                log.info("[DEV-MAIL] to={} subject=\"{}\"\n{}",
                        Arrays.toString(msg.getTo()), msg.getSubject(), msg.getText());
            }
        };
    }
}
