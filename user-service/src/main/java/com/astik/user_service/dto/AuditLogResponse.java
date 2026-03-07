package com.astik.user_service.dto;

import com.astik.user_service.enums.AuditAction;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        AuditAction action,
        String entityName,
        Long entityId, // Entity mein aapne Long rakha hai, UUID nahi
        String oldValues,
        String newValues,
        String changedFields,
        String performedBy,
        String performedByRole,
        String ipAddress,
        String userAgent,
        String requestUri,
        String requestMethod,
        String description,
        Boolean success,
        String failureReason,
        LocalDateTime createdAt
) {}
