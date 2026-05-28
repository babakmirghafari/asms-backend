package com.asms.repository;

import com.asms.domain.Membership;
import com.asms.domain.enums.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    /**
     * Returns an active (non-REMOVED) membership for the given user and organization.
     * Used by AuthService for role resolution — REMOVED memberships are excluded so that
     * removed users do not have roles resolved for them.
     */
    @Query("""
            SELECT m FROM Membership m
            JOIN FETCH m.user
            JOIN FETCH m.organization
            WHERE m.user.id = :userId
              AND m.organization.id = :organizationId
              AND m.status != :removedStatus
            """)
    Optional<Membership> findByUserIdAndOrganizationId(
            @Param("userId") UUID userId,
            @Param("organizationId") UUID organizationId,
            @Param("removedStatus") MembershipStatus removedStatus);

    /**
     * Returns the membership row regardless of status, including REMOVED.
     * Used by {@code MembershipsService.createMembership} to detect and reactivate
     * a previously removed row (avoiding a duplicate-key violation).
     */
    @Query("""
            SELECT m FROM Membership m
            JOIN FETCH m.user
            JOIN FETCH m.organization
            WHERE m.user.id = :userId
              AND m.organization.id = :organizationId
            """)
    Optional<Membership> findByUserIdAndOrganizationIdIncludingRemoved(
            @Param("userId") UUID userId,
            @Param("organizationId") UUID organizationId);

    /**
     * Counts how many ACTIVE memberships a user has across ALL organizations.
     * Used by the minimum-membership guard in {@code MembershipsService.deleteMembership}.
     */
    @Query("SELECT COUNT(m) FROM Membership m WHERE m.user.id = :userId AND m.status = :activeStatus")
    long countActiveByUserId(@Param("userId") UUID userId, @Param("activeStatus") MembershipStatus activeStatus);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    Page<Membership> findByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Membership> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT m FROM Membership m JOIN FETCH m.user JOIN FETCH m.organization WHERE m.id = :id")
    Optional<Membership> findByIdWithAssociations(@Param("id") UUID id);

    @Query(value = """
            SELECT m FROM Membership m
            JOIN FETCH m.user
            JOIN FETCH m.organization
            WHERE (:orgId IS NULL OR m.organization.id = :orgId)
              AND (:userId IS NULL OR m.user.id = :userId)
              AND m.status != :removedStatus
            """,
            countQuery = """
            SELECT COUNT(m) FROM Membership m
            WHERE (:orgId IS NULL OR m.organization.id = :orgId)
              AND (:userId IS NULL OR m.user.id = :userId)
              AND m.status != :removedStatus
            """)
    Page<Membership> findFiltered(@Param("orgId") UUID orgId,
                                   @Param("userId") UUID userId,
                                   @Param("removedStatus") MembershipStatus removedStatus,
                                   Pageable pageable);
}
