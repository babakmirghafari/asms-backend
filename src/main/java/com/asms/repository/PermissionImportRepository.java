package com.asms.repository;

import com.asms.domain.PermissionImport;
import com.asms.domain.enums.PermissionImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link PermissionImport} import sessions.
 *
 * <p>Used by {@link com.asms.service.PermissionsService} for the two-step CSV import flow:
 * validate → commit (ARC42 §6 Flow 6).
 */
@Repository
public interface PermissionImportRepository extends JpaRepository<PermissionImport, UUID> {

    /**
     * Find an import session by ID, but only if it is still in
     * {@link PermissionImportStatus#PENDING_COMMIT} and has not expired.
     * Used by the commit step to guard against double-commit and TTL expiry.
     */
    Optional<PermissionImport> findByIdAndStatusAndExpiresAtAfter(
            UUID id,
            PermissionImportStatus status,
            OffsetDateTime now);

    /**
     * Mark all {@code PENDING_COMMIT} sessions that have passed their {@code expiresAt}
     * as {@code EXPIRED}. Called by a scheduled job (or on-demand) to clean up stale sessions.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE PermissionImport pi
            SET pi.status = :expired,
                pi.updatedAt = :now
            WHERE pi.status = :pendingCommit
              AND pi.expiresAt < :now
            """)
    int expireStaleSessions(@Param("now") OffsetDateTime now,
                            @Param("expired") PermissionImportStatus expired,
                            @Param("pendingCommit") PermissionImportStatus pendingCommit);

    /**
     * Find all import sessions for an organization, ordered by creation time descending.
     * Used by the report endpoint to support pagination in future iterations.
     */
    List<PermissionImport> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
