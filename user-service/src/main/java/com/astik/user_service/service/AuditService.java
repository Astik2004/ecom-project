package com.astik.user_service.service;

import com.astik.user_service.enums.AuditAction;

import java.util.UUID;

public interface AuditService {

    void log(AuditAction action, String entityName, UUID entityId,
             String performedBy, String ipAddress, String userAgent,
             Boolean success, String failureReason);
}