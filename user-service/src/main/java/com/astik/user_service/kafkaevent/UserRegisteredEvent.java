package com.astik.user_service.kafkaevent;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String role,
        LocalDateTime registeredAt
) {
    public static UserRegisteredEvent of(UUID userId, String firstName,
                                         String lastName, String email, String role) {
        return new UserRegisteredEvent(userId, firstName, lastName,
                email, role, LocalDateTime.now());
    }
}
