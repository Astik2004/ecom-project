package com.astik.user_service.repository;

import com.astik.user_service.entity.AuditLog;
import com.astik.user_service.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityIdOrderByCreatedAtDesc(UUID entityId);
    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);
    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);
}