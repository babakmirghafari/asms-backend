package com.asms.repository;

import com.asms.domain.OrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, UUID> {
    Optional<OrganizationSettings> findByOrganizationId(UUID organizationId);
}
