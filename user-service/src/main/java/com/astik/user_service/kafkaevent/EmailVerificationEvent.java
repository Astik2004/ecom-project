package com.astik.user_service.kafkaevent;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailVerificationEvent(
        UUID userId,
        String email,
        String firstName,
        String verificationToken,
        LocalDateTime expiresAt
) {
    public static EmailVerificationEvent of(UUID userId, String email,
                                            String firstName, String token,
                                            LocalDateTime expiresAt) {
        return new EmailVerificationEvent(userId, email, firstName, token, expiresAt);
    }
}