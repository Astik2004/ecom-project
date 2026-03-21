package com.astik.user_service.kafkaevent;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordResetEvent(
        UUID userId,
        String email,
        String firstName,
        String resetToken,
        LocalDateTime expiresAt
) {
    public static PasswordResetEvent of(UUID userId, String email,
                                        String firstName, String token,
                                        LocalDateTime expiresAt) {
        return new PasswordResetEvent(userId, email, firstName, token, expiresAt);
    }
}