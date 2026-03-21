package com.astik.notification_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailVerificationEvent(
        UUID userId,
        String email,
        String firstName,
        String verificationToken,
        LocalDateTime expiresAt
) {}