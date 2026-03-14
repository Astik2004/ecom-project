package com.astik.user_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application.account")
@Getter
@Setter
public class AccountProperties {
    private int maxFailedAttempts = 5;
    private long lockDurationMinutes = 30;
    private int emailTokenExpiryHours = 24;
    private int passwordResetTokenExpiryMinutes = 15;
}