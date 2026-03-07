package com.astik.user_service.service;

import com.astik.user_service.enums.AuditAction;

import java.util.UUID;

public interface AuditService {

    /**
     * Logs a successful action for a given user and entity.
     */
    void logSuccess(UUID userId,
                    String performedBy,
                    AuditAction action,
                    String entityName,
                    Long entityId,
                    String ipAddress);

    /**
     * Logs a failed action for a given user and entity.
     */
    void logFailure(UUID userId,
                    String performedBy,
                    AuditAction action,
                    String entityName,
                    String failureReason,
                    String ipAddress);

    /**
     * Logs an action with detailed old and new values and changed fields.
     */
    void logWithDetails(UUID userId,
                        String performedBy,
                        AuditAction action,
                        String entityName,
                        Long entityId,
                        String oldValues,
                        String newValues,
                        String changedFields,
                        String ipAddress,
                        String userAgent);
}