package com.asms.repository;

import com.asms.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findByOrgIdAndId(UUID orgId, UUID id);

    /**
     * Finds the most recently created audit log entry per org.
     * Used by the hash-chain algorithm to obtain the previous row's hash.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.orgId = :orgId
            ORDER BY a.createdAt DESC
            LIMIT 1
            """)
    Optional<AuditLog> findLatestByOrgId(@Param("orgId") UUID orgId);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.orgId = :orgId
              AND (:actorId IS NULL OR a.actorId = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findFiltered(@Param("orgId") UUID orgId,
                                 @Param("actorId") UUID actorId,
                                 @Param("action") String action,
                                 @Param("from") OffsetDateTime from,
                                 @Param("to") OffsetDateTime to,
                                 Pageable pageable);

    long countByOrgIdAndCreatedAtAfter(UUID orgId, OffsetDateTime since);

    /**
     * Activity log query — pre-filters on action prefix 'ACTIVITY_'.
     * Backs ActivityLogsService without a separate table.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.orgId = :orgId
              AND a.action LIKE 'ACTIVITY_%'
              AND (:actorId IS NULL OR a.actorId = :actorId)
              AND (:category IS NULL OR a.targetType = :category)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findActivityLogs(@Param("orgId") UUID orgId,
                                     @Param("actorId") UUID actorId,
                                     @Param("category") String category,
                                     @Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to,
                                     Pageable pageable);
}
