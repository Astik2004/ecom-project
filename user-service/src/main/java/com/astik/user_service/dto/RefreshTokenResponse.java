package com.astik.user_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshTokenResponse(
        UUID id,             // BaseEntity id
        String token,        // Partially masked token dikhana safe hota hai
        boolean revoked,
        boolean expired,
        boolean isValid,     // entity ka isValid() method map karne ke liye
        LocalDateTime expiresAt,
        UUID userId,         // Kis user ka token hai
        LocalDateTime createdAt
) {}
