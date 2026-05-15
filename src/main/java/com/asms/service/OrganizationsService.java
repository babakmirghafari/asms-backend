package com.asms.service;

import com.asms.domain.Organization;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateOrganizationRequestDto;
import com.asms.model.OrganizationDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateOrganizationRequestDto;
import com.asms.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
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
    public ResponseEntity<OrganizationDto> createOrganization(CreateOrganizationRequestDto req) {
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
        auditService.recordInfo("ORGANIZATION", saved.getId(), "ORGANIZATION_CREATED", null, toDto(saved));
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Transactional
    public ResponseEntity<Void> deleteOrganization(UUID organizationId) {
        log.debug("Soft-delete organization: {}", organizationId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        OrganizationDto before = toDto(org);
        org.setStatus("DELETED");
        org.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", organizationId, "ORGANIZATION_DELETED", before, toDto(org));
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<OrganizationDto> getOrganizationById(UUID organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .filter(o -> !"DELETED".equals(o.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        return ResponseEntity.ok(toDto(org));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listOrganizations(Integer page, Integer size, String search) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        Page<Organization> results = organizationRepository.findAllActive(search, pageable);
        return ResponseEntity.ok(buildPagedResponse(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    @Transactional
    public ResponseEntity<OrganizationDto> updateOrganization(
            UUID organizationId, UpdateOrganizationRequestDto req) {
        log.debug("Update organization: {}", organizationId);

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        OrganizationDto before = toDto(org);
        if (req.getName() != null)    org.setName(req.getName());
        if (req.getLogoUrl() != null) org.setLogoUrl(req.getLogoUrl());
        if (req.getStatus() != null)  org.setStatus(req.getStatus().getValue());
        org.setUpdatedAt(OffsetDateTime.now());

        Organization saved = organizationRepository.save(org);
        auditService.recordInfo("ORGANIZATION", organizationId, "ORGANIZATION_UPDATED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private OrganizationDto toDto(Organization org) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setLogoUrl(org.getLogoUrl());
        dto.setParentOrganizationId(org.getParentOrgId());
        dto.setStatus(OrganizationDto.StatusEnum.fromValue(org.getStatus()));
        dto.setCreatedAt(org.getCreatedAt());
        dto.setUpdatedAt(org.getUpdatedAt());
        return dto;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private PagedResponseDto buildPagedResponse(List<?> items, long total, int page, int size) {
        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(items.stream().map(i -> (Object) i).toList());
        dto.setTotalElements(total);
        dto.setNumber(page);
        dto.setSize(size);
        dto.setTotalPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return dto;
    }
}
