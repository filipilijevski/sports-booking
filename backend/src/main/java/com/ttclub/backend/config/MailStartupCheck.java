package com.ttclub.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
@ConditionalOnProperty(name = "ttclub.mail.startup-check", havingValue = "true", matchIfMissing = false)
public class MailStartupCheck {

    private static final Logger log = LoggerFactory.getLogger(MailStartupCheck.class);
    private final JavaMailSender sender;

    public MailStartupCheck(JavaMailSender sender) {
        this.sender = sender;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        if (sender instanceof JavaMailSenderImpl impl) {
            try {
                impl.testConnection();
                log.info("Mail connection test succeeded.");
            } catch (Exception e) {
                throw new IllegalStateException("Mail configuration invalid - cannot connect to SMTP server", e);
            }
        } else {
            log.info("Mail sender is not JavaMailSenderImpl - skipping SMTP connection test.");
        }
    }
}
