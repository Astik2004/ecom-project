package com.astik.notification_service.consumer;

import com.astik.notification_service.dto.EmailVerificationEvent;
import com.astik.notification_service.dto.PasswordResetEvent;
import com.astik.notification_service.dto.UserRegisteredEvent;
import com.astik.notification_service.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    // ── User Registered ───────────────────────────────────────────
    @KafkaListener(
            topics  = "${app.kafka.topics.user-registered}",
            groupId = "notification-group"
    )
    public void handleUserRegistered(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("user.registered received | offset={}", offset);
        try {
            UserRegisteredEvent event = objectMapper
                    .readValue(message, UserRegisteredEvent.class);
            emailService.sendWelcomeEmail(event);
        } catch (Exception e) {
            log.error("Failed to process user.registered | error={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ── Email Verification ────────────────────────────────────────
    @KafkaListener(
            topics  = "${app.kafka.topics.email-verification}",
            groupId = "notification-group"
    )
    public void handleEmailVerification(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("email.verification received | offset={}", offset);
        try {
            EmailVerificationEvent event = objectMapper
                    .readValue(message, EmailVerificationEvent.class);
            emailService.sendVerificationEmail(event);
        } catch (Exception e) {
            log.error("Failed to process email.verification | error={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ── Password Reset ────────────────────────────────────────────
    @KafkaListener(
            topics  = "${app.kafka.topics.password-reset}",
            groupId = "notification-group"
    )
    public void handlePasswordReset(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("password.reset received | offset={}", offset);
        try {
            PasswordResetEvent event = objectMapper
                    .readValue(message, PasswordResetEvent.class);
            emailService.sendPasswordResetEmail(event);
        } catch (Exception e) {
            log.error("Failed to process password.reset | error={}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ── DLT ──────────────────────────────────────────────────────
    @KafkaListener(
            topics  = "${app.kafka.topics.user-registered}.DLT",
            groupId = "notification-dlt-group"
    )
    public void handleDLT(
            @Payload byte[] message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Message in DLT | topic={}", topic);
    }
}