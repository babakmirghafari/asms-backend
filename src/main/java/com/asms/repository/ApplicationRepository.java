package com.asms.repository;

import com.asms.domain.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByOrgIdAndId(UUID orgId, UUID id);

    boolean existsByOrgIdAndName(UUID orgId, String name);

    @Query("""
            SELECT a FROM Application a
            WHERE a.orgId = :orgId
              AND a.status != 'DELETED'
              AND (:type IS NULL OR a.type = :type)
            """)
    Page<Application> findFiltered(@Param("orgId") UUID orgId,
                                    @Param("type") String type,
                                    Pageable pageable);
}
