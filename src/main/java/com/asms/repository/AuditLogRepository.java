package com.asms.repository;

import com.asms.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link AuditLog} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * via {@link AuditLogSpecifications}, avoiding the PostgreSQL
 * "could not determine data type of parameter" error that occurs with
 * JPQL null-typed bound parameters.
 *
 * <p>Callers should use {@link AuditLogSpecifications} to build predicates
 * and call {@link #findAll(org.springframework.data.jpa.domain.Specification, Pageable)}
 * rather than calling a fixed JPQL query with nullable parameters.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>,
        JpaSpecificationExecutor<AuditLog> {

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

    long countByOrgIdAndCreatedAtAfter(UUID orgId, OffsetDateTime since);
}
