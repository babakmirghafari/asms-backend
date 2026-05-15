package com.asms.repository;

import com.asms.domain.StationPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationPolicyRepository extends JpaRepository<StationPolicy, UUID> {

    Optional<StationPolicy> findByOrgIdAndId(UUID orgId, UUID id);

    List<StationPolicy> findByUserId(UUID userId);

    Page<StationPolicy> findByUserId(UUID userId, Pageable pageable);

    Page<StationPolicy> findByOrgId(UUID orgId, Pageable pageable);
}
