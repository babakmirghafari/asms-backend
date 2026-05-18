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
     */
    @Query(value = """
            SELECT u.* FROM users u
            WHERE u.status != :deletedKey
              AND EXISTS (
                  SELECT 1 FROM memberships m
                  WHERE m.user_id = u.id
                    AND m.org_id = :orgId
                    AND m.status = :activeKey
              )
              AND (:search IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.last_name)  LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(*) FROM users u
            WHERE u.status != :deletedKey
              AND EXISTS (
                  SELECT 1 FROM memberships m
                  WHERE m.user_id = u.id
                    AND m.org_id = :orgId
                    AND m.status = :activeKey
              )
              AND (:search IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.last_name)  LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            nativeQuery = true)
    Page<User> findAllByOrgId(@Param("orgId") UUID orgId,
                               @Param("search") String search,
                               @Param("deletedKey") int deletedKey,
                               @Param("activeKey") int activeKey,
                               Pageable pageable);

    long countByStatus(UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status != :deleted")
    long countNonDeleted(@Param("deleted") UserStatus deleted);
}
