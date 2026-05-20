package com.asms.repository;

import com.asms.domain.Alert;
import com.asms.domain.enums.AlertSeverity;
import com.asms.domain.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByOrganizationIdAndId(UUID organizationId, UUID id);

    Page<Alert> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.organization.id = :orgId
              AND (:type IS NULL OR a.type = :type)
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.createdAt DESC
            """)
    Page<Alert> findFiltered(@Param("orgId") UUID orgId,
                              @Param("type") String type,
                              @Param("status") AlertStatus status,
                              Pageable pageable);

    @Query("""
            SELECT COUNT(a) FROM Alert a
            WHERE a.organization.id = :orgId AND a.status = :status AND a.severity = :severity
            """)
    long countOpenBySeverity(@Param("orgId") UUID orgId,
                              @Param("status") AlertStatus status,
                              @Param("severity") AlertSeverity severity);
}
