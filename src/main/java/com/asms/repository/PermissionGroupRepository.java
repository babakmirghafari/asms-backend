package com.asms.repository;

import com.asms.domain.PermissionGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, UUID> {

    Optional<PermissionGroup> findByOrgIdAndId(UUID orgId, UUID id);

    boolean existsByOrgIdAndName(UUID orgId, String name);

    Page<PermissionGroup> findByOrgId(UUID orgId, Pageable pageable);

    /**
     * Returns all groups the user belongs to within an org.
     * Used by the effective-permission computation (ADR-008).
     */
    @Query("""
            SELECT pg FROM PermissionGroup pg
            JOIN pg.members u
            WHERE u.id = :userId
              AND pg.orgId = :orgId
            """)
    List<PermissionGroup> findGroupsForUser(@Param("userId") UUID userId,
                                             @Param("orgId") UUID orgId);
}
