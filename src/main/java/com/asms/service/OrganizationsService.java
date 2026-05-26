package com.asms.service;

import com.asms.domain.Organization;
import com.asms.exception.ResourceNotFoundException;
import com.asms.domain.enums.OrganizationStatus;
import com.asms.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Multi-tenant organization management domain service (AC-3, AC-12, AC-13).
 *
 * <p>Per ADR-006: enforces row-level org isolation on all queries.
 * Per ADR-009: every write operation records a tamper-resistant audit event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationsService {

    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    /**
     * Persists a new organization. The handler converts the request DTO to an
     * {@link Organization} entity via the mapper before calling here.
     */
    @Transactional
    public Organization createOrganization(Organization org, UUID parentOrgId) {
        log.debug("Create organization: {}", org.getName());
        if (parentOrgId != null) {
            Organization parent = organizationRepository.findById(parentOrgId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent organization not found: " + parentOrgId));
            org.setParentOrganization(parent);
        }
        Organization saved = organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", saved.getId(), "ORGANIZATION_CREATED", null, saved);
        return saved;
    }

    @Transactional
    public void deleteOrganization(UUID organizationId) {
        log.debug("Soft-delete organization: {}", organizationId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        org.setStatus(OrganizationStatus.DELETED);
        org.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", organizationId, "ORGANIZATION_DELETED", null, org);
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .filter(o -> OrganizationStatus.DELETED != o.getStatus())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    }

    @Transactional(readOnly = true)
    public Page<Organization> listOrganizations(Integer page, Integer size, String search) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        return organizationRepository.findAllActive(search, pageable);
    }

    /**
     * Applies updates to an existing organization.
     * The handler extracts primitive fields from the request DTO before calling here.
     *
     * @param organizationId target organization identifier
     * @param patch          partial Organization carrying the fields to update (null fields not applied)
     */
    @Transactional
    public Organization updateOrganization(UUID organizationId, Organization patch) {
        log.debug("Update organization: {}", organizationId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        if (patch.getName() != null)        org.setName(patch.getName());
        if (patch.getDescription() != null) org.setDescription(patch.getDescription());
        if (patch.getLogoUrl() != null)     org.setLogoUrl(patch.getLogoUrl());
        if (patch.getStatus() != null)      org.setStatus(patch.getStatus());
        org.setUpdatedAt(OffsetDateTime.now());

        Organization saved = organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", organizationId, "ORGANIZATION_UPDATED", null, saved);
        return saved;
    }

}
