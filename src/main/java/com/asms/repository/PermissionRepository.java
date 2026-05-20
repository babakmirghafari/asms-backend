package com.asms.repository;

import com.asms.domain.Permission;
import com.asms.domain.enums.PermissionStatus;
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
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByOrganizationIdAndId(UUID organizationId, UUID id);

    boolean existsByOrganizationIdAndName(UUID organizationId, String name);

    Page<Permission> findByOrganizationIdAndStatus(UUID organizationId, PermissionStatus status, Pageable pageable);

    Page<Permission> findByOrganizationId(UUID organizationId, Pageable pageable);

    /**
     * Returns all ACTIVE permissions directly assigned to a user via any group,
     * for the given org — used by the effective-permission computation (ADR-008).
     */
    @Query("""
            SELECT DISTINCT p FROM Permission p
            JOIN p.permissionGroups pg
            JOIN pg.members u
            WHERE u.id = :userId
              AND pg.organization.id = :orgId
              AND p.status = :active
            """)
    List<Permission> findGroupPermissionsForUser(@Param("userId") UUID userId,
                                                  @Param("orgId") UUID orgId,
                                                  @Param("active") PermissionStatus active);

    /**
     * Returns all ACTIVE permissions in an org, for CSV export / simulation.
     */
    List<Permission> findByOrganizationIdAndStatus(UUID organizationId, PermissionStatus status);
}
