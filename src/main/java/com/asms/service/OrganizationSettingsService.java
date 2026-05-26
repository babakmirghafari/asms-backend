package com.asms.service;

import com.asms.domain.Organization;
import com.asms.domain.OrganizationSettings;
import com.asms.exception.ResourceNotFoundException;
import com.asms.mapper.OrganizationSettingsMapper;
import com.asms.model.OrganizationSettingsDto;
import com.asms.model.UpdateOrganizationSettingsRequestDto;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.OrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Business logic for Organization Settings — security, SSO, branding, and compliance.
 *
 * <p>If no settings row exists for an organization, {@link #getSettings} lazily creates
 * a default row (all-false booleans, session_timeout=30, max_concurrent_sessions=3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationSettingsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository organizationSettingsRepository;
    private final OrganizationSettingsMapper organizationSettingsMapper;
    private final AuditService auditService;

    /**
     * Returns the settings for the given organization.
     * If no row exists yet, a default settings row is created and persisted.
     *
     * @param orgId organization identifier
     * @return the organization's settings DTO
     * @throws ResourceNotFoundException if the organization does not exist
     */
    @Transactional
    public OrganizationSettingsDto getSettings(UUID orgId) {
        Organization org = requireOrganization(orgId);
        OrganizationSettings settings = organizationSettingsRepository.findByOrganizationId(orgId)
                .orElseGet(() -> createDefaultSettings(org));
        return organizationSettingsMapper.toDto(settings);
    }

    /**
     * Updates settings for the given organization.
     * Only fields present in the request body (non-null) are applied.
     *
     * @param orgId organization identifier
     * @param dto   the fields to update
     * @return the updated settings DTO
     * @throws ResourceNotFoundException if the organization does not exist
     */
    @Transactional
    public OrganizationSettingsDto updateSettings(UUID orgId, UpdateOrganizationSettingsRequestDto dto) {
        log.debug("Update organization settings: orgId={}", orgId);
        Organization org = requireOrganization(orgId);

        OrganizationSettings settings = organizationSettingsRepository.findByOrganizationId(orgId)
                .orElseGet(() -> createDefaultSettings(org));

        organizationSettingsMapper.patch(dto, settings);
        settings.setUpdatedAt(OffsetDateTime.now());

        OrganizationSettings saved = organizationSettingsRepository.save(settings);
        auditService.recordInfo("ORGANIZATION_SETTINGS", orgId, "ORGANIZATION_SETTINGS_UPDATED", null, saved);

        return organizationSettingsMapper.toDto(saved);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private Organization requireOrganization(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
    }

    private OrganizationSettings createDefaultSettings(Organization org) {
        log.debug("Creating default organization settings for orgId={}", org.getId());
        OrganizationSettings defaults = OrganizationSettings.builder()
                .organization(org)
                .requireMfa(false)
                .forceMfaOnSensitive(false)
                .sessionTimeout(30)
                .maxConcurrentSessions(3)
                .ssoEnabled(false)
                .autoProvision(false)
                .autoDeprovision(false)
                .enforceDataResidencyOnExports(false)
                .longTermAuditRetention(false)
                .gdprDataExportEndpoint(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return organizationSettingsRepository.save(defaults);
    }
}
