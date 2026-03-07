package com.astik.user_service.mapper;

import com.astik.user_service.dto.AuditLogResponse;
import com.astik.user_service.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog log) {
        if (log == null) return null;

        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityName(),
                log.getEntityId(),
                log.getOldValues(),
                log.getNewValues(),
                log.getChangedFields(),
                log.getPerformedBy(),
                log.getPerformedByRole(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getRequestUri(),
                log.getRequestMethod(),
                log.getDescription(),
                log.getSuccess(),
                log.getFailureReason(),
                log.getCreatedAt()
        );
    }
}