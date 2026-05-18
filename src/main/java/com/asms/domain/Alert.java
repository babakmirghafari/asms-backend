package com.asms.domain;

import com.asms.domain.enums.AlertRiskLevel;
import com.asms.domain.enums.AlertSeverity;
import com.asms.domain.enums.AlertStatus;
import com.asms.domain.enums.converter.AlertRiskLevelConverter;
import com.asms.domain.enums.converter.AlertSeverityConverter;
import com.asms.domain.enums.converter.AlertStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "severity", nullable = false)
    @Convert(converter = AlertSeverityConverter.class)
    private AlertSeverity severity;

    @Column(name = "status", nullable = false)
    @Convert(converter = AlertStatusConverter.class)
    private AlertStatus status;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "risk_score")
    private BigDecimal riskScore;

    @Column(name = "risk_level")
    @Convert(converter = AlertRiskLevelConverter.class)
    private AlertRiskLevel riskLevel;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "escalated_by")
    private UUID escalatedBy;

    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;

    @Column(name = "escalation_reason")
    private String escalationReason;

    @Column(name = "escalated_to")
    private String escalatedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
