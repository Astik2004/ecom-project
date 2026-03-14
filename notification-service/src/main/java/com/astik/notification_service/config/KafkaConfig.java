package com.astik.notification_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {

        // Failed message → DLT topic mein jaayega
        // e.g. user.registered.DLT
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 3 retries, har retry ke beech 2 second wait
        var backoff = new FixedBackOff(2000L, 3L);

        var errorHandler = new DefaultErrorHandler(recoverer, backoff);

        // In exceptions pe retry mat karo — seedha DLT bhejo
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        return errorHandler;
    }
}