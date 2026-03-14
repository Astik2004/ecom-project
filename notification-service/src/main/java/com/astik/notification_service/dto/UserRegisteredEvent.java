package com.astik.notification_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String role,
        LocalDateTime registeredAt
) {}