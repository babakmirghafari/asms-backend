package com.asms.domain;

import com.asms.domain.enums.PermissionImportStatus;
import com.asms.domain.enums.converter.PermissionImportStatusConverter;
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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Backing entity for the two-step CSV permission import flow (ARC42 §6 Flow 6).
 */
@Entity
@Table(name = "permission_imports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionImport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "status", nullable = false)
    @Convert(converter = PermissionImportStatusConverter.class)
    private PermissionImportStatus status;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "valid_rows")
    private Integer validRows;

    @Column(name = "error_rows")
    private Integer errorRows;

    @Column(name = "warning_rows")
    private Integer warningRows;

    @Column(name = "raw_csv_content", columnDefinition = "TEXT")
    private String rawCsvContent;

    @Column(name = "issues_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String issuesJson;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "committed_at")
    private OffsetDateTime committedAt;

    @Column(name = "committed_count")
    private Integer committedCount;

    @Column(name = "skipped_count")
    private Integer skippedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
