package com.auth.demo.service.impl;

import com.auth.demo.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service implementation.
 *
 * <ul>
 *   <li>When {@code app.mail.verification-enabled=false} (default/dev): logs the verification URL to the console.</li>
 *   <li>When {@code app.mail.verification-enabled=true} (production): sends a real email via SMTP.</li>
 * </ul>
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${app.mail.verification-enabled}")
    private boolean mailEnabled;

    @Value("${app.verification.base-url}")
    private String baseUrl;

    private final JavaMailSender mailSender;

    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = baseUrl + "/verify?token=" + token;

        if (!mailEnabled || mailSender == null) {
            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║         EMAIL VERIFICATION LINK (dev mode)              ║");
            log.info("╠══════════════════════════════════════════════════════════╣");
            log.info("║  To:  {}", to);
            log.info("║  URL: {}", verificationUrl);
            log.info("╚══════════════════════════════════════════════════════════╝");
            return;
        }

        // Production: send real email via SMTP
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verify Your Email Address");
        message.setText(
                "Welcome! Please click the link below to verify your email address:\n\n"
                        + verificationUrl + "\n\n"
                        + "This link will expire in 24 hours.\n\n"
                        + "If you did not create an account, please ignore this email."
        );

        mailSender.send(message);
        log.info("Verification email sent to: {}", to);
    }
}
