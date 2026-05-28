package com.asms.repository;

import com.asms.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query(value = """
            SELECT o.*,
                   (SELECT COUNT(m.id) FROM memberships m
                    WHERE m.org_id = o.id AND m.status != 4) AS membercount
            FROM organizations o
            WHERE o.status != 3
              AND (:search IS NULL
                   OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.slug) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY o.name ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM organizations o
            WHERE o.status != 3
              AND (:search IS NULL
                   OR LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.slug) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            nativeQuery = true)
    Page<Organization> findAllActive(@Param("search") String search, Pageable pageable);
}
