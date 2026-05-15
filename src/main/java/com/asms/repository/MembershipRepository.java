package com.asms.repository;

import com.asms.domain.Membership;
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

    Optional<Membership> findByUserIdAndOrgId(UUID userId, UUID orgId);

    boolean existsByUserIdAndOrgId(UUID userId, UUID orgId);

    Page<Membership> findByOrgId(UUID orgId, Pageable pageable);

    Page<Membership> findByUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT m FROM Membership m
            WHERE (:orgId IS NULL OR m.orgId = :orgId)
              AND (:userId IS NULL OR m.userId = :userId)
            """)
    Page<Membership> findFiltered(@Param("orgId") UUID orgId,
                                   @Param("userId") UUID userId,
                                   Pageable pageable);
}
