package com.asms.service;

import com.asms.domain.Organization;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateOrganizationRequestDto;
import com.asms.model.UpdateOrganizationRequestDto;
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

    @Transactional
    public Organization createOrganization(CreateOrganizationRequestDto req) {
        log.debug("Create organization: {}", req.getName());

        Organization org = Organization.builder()
                .name(req.getName())
                .slug(generateSlug(req.getName()))
                .parentOrgId(req.getParentOrganizationId())
                .logoUrl(req.getLogoUrl())
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        Organization saved = organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", saved.getId(), "ORGANIZATION_CREATED", null, saved);
        return saved;
    }

    @Transactional
    public void deleteOrganization(UUID organizationId) {
        log.debug("Soft-delete organization: {}", organizationId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        org.setStatus("DELETED");
        org.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", organizationId, "ORGANIZATION_DELETED", null, org);
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .filter(o -> !"DELETED".equals(o.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
    }

    @Transactional(readOnly = true)
    public Page<Organization> listOrganizations(Integer page, Integer size, String search) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        return organizationRepository.findAllActive(search, pageable);
    }

    @Transactional
    public Organization updateOrganization(UUID organizationId, UpdateOrganizationRequestDto req) {
        log.debug("Update organization: {}", organizationId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        if (req.getName() != null)    org.setName(req.getName());
        if (req.getLogoUrl() != null) org.setLogoUrl(req.getLogoUrl());
        if (req.getStatus() != null)  org.setStatus(req.getStatus().getValue());
        org.setUpdatedAt(OffsetDateTime.now());

        Organization saved = organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", organizationId, "ORGANIZATION_UPDATED", null, saved);
        return saved;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

}
