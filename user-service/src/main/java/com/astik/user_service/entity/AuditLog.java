package com.astik.user_service.entity;

import com.astik.user_service.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_entity_id",   columnList = "entity_id"),
                @Index(name = "idx_audit_action",      columnList = "action"),
                @Index(name = "idx_audit_created_at",  columnList = "created_at"),
                @Index(name = "idx_audit_performed_by",columnList = "performed_by")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_values", length = 4000)
    private String oldValues;

    @Column(name = "new_values", length = 4000)
    private String newValues;

    @Column(name = "changed_fields", length = 1000)
    private String changedFields;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "performed_by_role", length = 30)
    private String performedByRole;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(length = 500)
    private String description;

    private Boolean success;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}