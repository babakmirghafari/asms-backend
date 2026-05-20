package com.asms.repository;

import com.asms.domain.AuthPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthPolicyRepository extends JpaRepository<AuthPolicy, UUID> {

    Optional<AuthPolicy> findByOrganizationId(UUID organizationId);
}
