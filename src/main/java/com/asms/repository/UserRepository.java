package com.asms.repository;

import com.asms.domain.User;
import com.asms.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Lists users who are members of the given org (via memberships table).
     * Org-scoped per ADR-006 / RISK-002 mitigation.
     *
     * Uses a native query to avoid Hibernate 6 type-inference issues with
     * null CONCAT parameters on PostgreSQL (lower(bytea) error).
     * Enum keys are passed as integers because native SQL bypasses the JPA converter.
     *
     * @param statusKey optional status filter — pass {@code null} to include all non-deleted statuses
     */
    @Query(value = """
            SELECT u.* FROM users u
            WHERE u.status != :deletedKey
              AND (:statusKey IS NULL OR u.status = :statusKey)
              AND (:search IS NULL
                   OR LOWER(u.username)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(*) FROM users u
            WHERE u.status != :deletedKey
              AND (:statusKey IS NULL OR u.status = :statusKey)
              AND EXISTS (
                  SELECT 1 FROM memberships m
                  WHERE m.user_id = u.id
                    AND m.org_id = :orgId
                    AND m.status = :activeKey
              )
              AND (:search IS NULL
                   OR LOWER(u.username)  LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            nativeQuery = true)
    Page<User> findAllByOrgId(@Param("orgId") UUID orgId,
                               @Param("search") String search,
                               @Param("statusKey") Integer statusKey,
                               @Param("deletedKey") int deletedKey,
                               @Param("activeKey") int activeKey,
                               Pageable pageable);

    long countByStatus(UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status != :deleted")
    long countNonDeleted(@Param("deleted") UserStatus deleted);

    /**
     * Fetches a user together with their permission groups and direct permissions in a
     * single query, avoiding N+1 issues when computing effective permissions.
     *
     * <p>Uses a named entity graph so the two join-fetch collections are loaded together.
     * Callers should use this instead of {@link #findById} whenever the permission
     * collections will be accessed.
     */
    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.permissionGroups pg
            LEFT JOIN FETCH pg.permissions
            LEFT JOIN FETCH u.directPermissions
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithGroupsAndPermissions(@Param("id") UUID id);
}
