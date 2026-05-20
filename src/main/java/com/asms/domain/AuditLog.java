package com.asms.domain;

import com.asms.domain.enums.AuditSeverity;
import com.asms.domain.enums.converter.AuditSeverityConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Append-only audit log entry per ADR-009.
 * UPDATE and DELETE are blocked via PostgreSQL trigger.
 * Hash chain: entry_hash = SHA-256(id + created_at + actor_id + action + before_state + after_state + previous_hash)
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_username")
    private String actorUsername;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    // Forensic UUID — no FK constraint in DDL
    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "action", nullable = false)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", columnDefinition = "JSONB")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", columnDefinition = "JSONB")
    private String afterState;

    @Column(name = "severity", nullable = false)
    @Convert(converter = AuditSeverityConverter.class)
    private AuditSeverity severity;

    @Column(name = "ip_address")
    private String ipAddress;

    // Forensic UUID — no FK constraint in DDL
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "previous_hash")
    private String previousHash;

    @Column(name = "entry_hash", nullable = false)
    private String entryHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
