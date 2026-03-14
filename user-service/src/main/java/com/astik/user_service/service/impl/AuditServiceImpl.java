// service/AuditService.java
package com.astik.user_service.service.impl;

import com.astik.user_service.entity.AuditLog;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.repository.AuditLogRepository;
import com.astik.user_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    // Async + separate transaction — audit never fails the main flow
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityName, UUID entityId,
                    String performedBy, String ipAddress, String userAgent,
                    Boolean success, String failureReason) {
        try {
            AuditLog log = AuditLog.builder()
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .performedBy(performedBy)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to save audit log | action={} entity={}", action, entityName, e);
        }
    }
}