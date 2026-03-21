package com.astik.notification_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordResetEvent(
        UUID userId,
        String email,
        String firstName,
        String resetToken,
        LocalDateTime expiresAt
) {}