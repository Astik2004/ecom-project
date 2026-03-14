package com.astik.notification_service.consumer;

import com.astik.notification_service.dto.UserRegisteredEvent;
import com.astik.notification_service.service.EmailService;
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

    @KafkaListener(
            topics   = "${app.kafka.topics.user-registered}",
            groupId  = "notification-group"
    )
    public void handleUserRegistered(
            @Payload UserRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Event received | topic={} partition={} offset={} userId={} email={}",
                topic, partition, offset, event.userId(), event.email());

        emailService.sendWelcomeEmail(event);
    }

    // DLT consumer — failed messages
    @KafkaListener(
            topics  = "${app.kafka.topics.user-registered}.DLT",
            groupId = "notification-dlt-group"
    )
    public void handleDLT(
            @Payload byte[] message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error("Message in DLT | topic={} — manual intervention needed", topic);
    }
}
