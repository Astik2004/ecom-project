package com.astik.user_service.service.impl;

import com.astik.user_service.entity.AuditLog;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.repository.AuditLogRepository;
import com.astik.user_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async("taskExecutor")
    public void logSuccess(UUID userId, String performedBy, AuditAction action,
                           String entityName, Long entityId, String ipAddress) {
        save(userId, performedBy, action, entityName, entityId,
                null, null, null, ipAddress, null, null, true);
    }

    @Async("taskExecutor")
    public void logFailure(UUID userId, String performedBy, AuditAction action,
                           String entityName, String failureReason, String ipAddress) {
        save(userId, performedBy != null ? performedBy : "ANONYMOUS", action,
                entityName, null, null, null, null, ipAddress, null, failureReason, false);
    }

    @Async("taskExecutor")
    public void logWithDetails(UUID userId, String performedBy, AuditAction action,
                               String entityName, Long entityId,
                               String oldValues, String newValues,
                               String changedFields, String ipAddress, String userAgent) {
        save(userId, performedBy, action, entityName, entityId,
                oldValues, newValues, changedFields, ipAddress, userAgent, null, true);
    }

    private void save(UUID id, String performedBy, AuditAction action,
                      String entityName, Long entityId,
                      String oldValues, String newValues, String changedFields,
                      String ipAddress, String userAgent,
                      String failureReason, Boolean success) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .id(id)
                    .performedBy(performedBy)
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .changedFields(changedFields)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .failureReason(failureReason)
                    .success(success)
                    .build();

            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save audit log [action={}]: {}", action, e.getMessage());
        }
    }
}