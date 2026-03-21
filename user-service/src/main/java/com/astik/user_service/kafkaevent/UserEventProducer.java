package com.astik.user_service.kafkaevent;

import com.astik.user_service.kafkaevent.EmailVerificationEvent;
import com.astik.user_service.kafkaevent.PasswordResetEvent;
import com.astik.user_service.kafkaevent.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Value("${app.kafka.topics.email-verification}")
    private String emailVerificationTopic;

    @Value("${app.kafka.topics.password-reset}")
    private String passwordResetTopic;

    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(userRegisteredTopic,
                        event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null)
                        log.info("Published user.registered | userId={}", event.userId());
                    else
                        log.error("Failed user.registered | userId={}", event.userId(), ex);
                });
    }

    public void publishEmailVerification(EmailVerificationEvent event) {
        kafkaTemplate.send(emailVerificationTopic,
                        event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null)
                        log.info("Published email.verification | userId={}", event.userId());
                    else
                        log.error("Failed email.verification | userId={}", event.userId(), ex);
                });
    }

    public void publishPasswordReset(PasswordResetEvent event) {
        kafkaTemplate.send(passwordResetTopic,
                        event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null)
                        log.info("Published password.reset | userId={}", event.userId());
                    else
                        log.error("Failed password.reset | userId={}", event.userId(), ex);
                });
    }
}