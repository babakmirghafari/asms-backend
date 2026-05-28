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

    @Query("SELECT m FROM Membership m JOIN FETCH m.user JOIN FETCH m.organization WHERE m.user.id = :userId AND m.organization.id = :organizationId")
    Optional<Membership> findByUserIdAndOrganizationId(@Param("userId") UUID userId, @Param("organizationId") UUID organizationId);

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
