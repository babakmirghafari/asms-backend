package com.asms.repository;

import com.asms.domain.Alert;
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

    Optional<Alert> findByOrgIdAndId(UUID orgId, UUID id);

    Page<Alert> findByOrgId(UUID orgId, Pageable pageable);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.orgId = :orgId
              AND (:type IS NULL OR a.type = :type)
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.createdAt DESC
            """)
    Page<Alert> findFiltered(@Param("orgId") UUID orgId,
                              @Param("type") String type,
                              @Param("status") String status,
                              Pageable pageable);

    @Query("""
            SELECT COUNT(a) FROM Alert a
            WHERE a.orgId = :orgId AND a.status = 'OPEN' AND a.severity = :severity
            """)
    long countOpenBySeverity(@Param("orgId") UUID orgId, @Param("severity") String severity);
}
