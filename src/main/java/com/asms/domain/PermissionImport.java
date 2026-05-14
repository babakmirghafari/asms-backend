package com.asms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Backing entity for the two-step CSV permission import flow (ARC42 §6 Flow 6).
 *
 * <p>An import session is created by {@code POST /permissions/import/validate}
 * and committed by {@code POST /permissions/import/commit}. Sessions that are not
 * committed within {@code IMPORT_TTL_MINUTES} minutes are considered expired.
 *
 * <p>The raw CSV content is stored encoded in {@link #rawCsvContent} so the commit step
 * can replay validation without requiring the client to re-upload the file.
 *
 * <p>Row-level validation issues are stored as JSON in {@link #issuesJson}
 * and deserialized on demand by the service layer.
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

    /** Organization to import permissions into. */
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /** Current status of this import session. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PermissionImportStatus status;

    /** Total number of data rows in the CSV (excluding header). */
    @Column(name = "total_rows")
    private Integer totalRows;

    /** Number of rows that passed validation. */
    @Column(name = "valid_rows")
    private Integer validRows;

    /** Number of rows with validation errors (will be skipped on commit). */
    @Column(name = "error_rows")
    private Integer errorRows;

    /** Number of rows with validation warnings (committed with notes unless skipWarningRows=true). */
    @Column(name = "warning_rows")
    private Integer warningRows;

    /**
     * Raw CSV content stored as UTF-8 text for replay during the commit step.
     * Stored as TEXT in Postgres; not exposed via API.
     */
    @Column(name = "raw_csv_content", columnDefinition = "TEXT")
    private String rawCsvContent;

    /**
     * Row-level validation issues serialized as a JSON array of
     * {@code PermissionImportRowIssueDto} objects. Deserialized by the service layer.
     */
    @Column(name = "issues_json", columnDefinition = "JSONB")
    private String issuesJson;

    /** UTC timestamp after which this session cannot be committed. */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** UTC timestamp when this session was committed. Null until committed. */
    @Column(name = "committed_at")
    private OffsetDateTime committedAt;

    /** Number of permissions actually committed. Null until committed. */
    @Column(name = "committed_count")
    private Integer committedCount;

    /** Number of rows skipped during commit. Null until committed. */
    @Column(name = "skipped_count")
    private Integer skippedCount;

    /** UTC timestamp when this record was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** UTC timestamp of the last status update. */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
