package com.astik.notification_service.service.impl;

import com.astik.notification_service.dto.UserRegisteredEvent;
import com.astik.notification_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Override
    @Async   // email sending async hai — consumer block nahi hoga
    public void sendWelcomeEmail(UserRegisteredEvent event) {
        try {
            // Thymeleaf context — template mein variables pass karo
            Context context = new Context();
            context.setVariable("firstName", event.firstName());
            context.setVariable("lastName",  event.lastName());
            context.setVariable("email",     event.email());
            context.setVariable("role",      event.role());

            // HTML template process karo
            String htmlContent = templateEngine
                    .process("welcome-email", context);

            sendEmail(
                    event.email(),
                    "Welcome to Ecommerce App, " + event.firstName() + "!",
                    htmlContent
            );

            log.info("Welcome email sent | userId={} email={}",
                    event.userId(), event.email());

        } catch (Exception e) {
            log.error("Failed to send welcome email | userId={} email={} error={}",
                    event.userId(), event.email(), e.getMessage());
            throw new RuntimeException("Email sending failed", e);
            // Exception throw karo taaki Kafka retry kar sake
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            Context context = new Context();
            context.setVariable("resetToken", resetToken);
            context.setVariable("resetLink",
                    "http://localhost:3000/reset-password?token=" + resetToken);

            String htmlContent = templateEngine
                    .process("password-reset-email", context);

            sendEmail(email, "Password Reset Request", htmlContent);
            log.info("Password reset email sent | email={}", email);

        } catch (Exception e) {
            log.error("Failed to send password reset email | email={}", email, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    // ── Private helper ────────────────────────────────────────────
    private void sendEmail(String to, String subject,
                           String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML
        mailSender.send(message);
    }
}