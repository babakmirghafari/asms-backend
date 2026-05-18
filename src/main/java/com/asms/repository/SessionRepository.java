package com.asms.repository;

import com.asms.domain.Session;
import com.asms.domain.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByOrgIdAndId(UUID orgId, UUID id);

    Optional<Session> findByTokenHash(String tokenHash);

    long countByOrgIdAndStatus(UUID orgId, SessionStatus status);

    Page<Session> findByOrgId(UUID orgId, Pageable pageable);

    @Query("""
            SELECT s FROM Session s
            WHERE s.orgId = :orgId
              AND (:userId IS NULL OR s.userId = :userId)
              AND (:status IS NULL OR s.status = :status)
            """)
    Page<Session> findFiltered(@Param("orgId") UUID orgId,
                                @Param("userId") UUID userId,
                                @Param("status") SessionStatus status,
                                Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Session s SET s.status = :revoked, s.revokedAt = :now
            WHERE s.userId = :userId AND s.status = :active
            """)
    int revokeAllForUser(@Param("userId") UUID userId,
                          @Param("now") OffsetDateTime now,
                          @Param("revoked") SessionStatus revoked,
                          @Param("active") SessionStatus active);
}
