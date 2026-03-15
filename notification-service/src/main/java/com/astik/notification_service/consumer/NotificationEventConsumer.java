package com.astik.notification_service.consumer;

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
    private final ObjectMapper objectMapper;   // inject karenge

    @KafkaListener(
            topics  = "${app.kafka.topics.user-registered}",
            groupId = "notification-group"
    )
    public void handleUserRegistered(
            @Payload String message,           // String receive karo
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Event received | topic={} partition={} offset={}",
                topic, partition, offset);

        try {
            // Manually parse karo String → UserRegisteredEvent
            UserRegisteredEvent event = objectMapper
                    .readValue(message, UserRegisteredEvent.class);

            log.info("Parsed event | userId={} email={}",
                    event.userId(), event.email());

            emailService.sendWelcomeEmail(event);

        } catch (Exception e) {
            log.error("Failed to parse event | message={} error={}",
                    message, e.getMessage());
            throw new RuntimeException("Event parsing failed", e);
        }
    }

    // DLT consumer
    @KafkaListener(
            topics  = "${app.kafka.topics.user-registered}.DLT",
            groupId = "notification-dlt-group"
    )
    public void handleDLT(
            @Payload byte[] message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Message in DLT | topic={} — manual check needed", topic);
    }
}