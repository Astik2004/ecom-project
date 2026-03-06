package com.astik.user_service.repository;

import com.astik.user_service.entity.AuditLog;
import com.astik.user_service.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByEntityIdOrderByCreatedAtDesc(Long entityId, Pageable pageable);

    Page<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy, Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.action IN ('LOGIN_FAILED', 'ACCOUNT_LOCKED')
        AND a.createdAt >= :since
        ORDER BY a.createdAt DESC
    """)
    List<AuditLog> findSecurityAlertsAfter(@Param("since") LocalDateTime since);

    List<AuditLog> findTop10ByPerformedByOrderByCreatedAtDesc(String performedBy);

    @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.createdAt DESC")
    Page<AuditLog> findAllFailedActions(Pageable pageable);
}