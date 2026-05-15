package com.asms.service;

import com.asms.domain.Application;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.ApplicationCredentialDto;
import com.asms.model.ApplicationDto;
import com.asms.model.CreateApplicationRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateApplicationRequestDto;
import com.asms.repository.ApplicationRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Client application registry service (AC-3, AC-12, AC-13).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationsService {

    private final ApplicationRepository applicationRepository;
    private final AuditService auditService;

    @Transactional
    public ResponseEntity<ApplicationDto> createApplication(CreateApplicationRequestDto req) {
        UUID orgId = req.getOrganizationId() != null ? req.getOrganizationId() : TenantContext.getRequiredOrgId();
        Application app = Application.builder()
                .orgId(orgId)
                .name(req.getName())
                .type(req.getConnectorType().getValue())
                .redirectUris(req.getRedirectUris())
                .samlEntityId(req.getSamlEntityId())
                .status("ACTIVE")
                .integrationHealthStatus("NEVER_CONNECTED")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        Application saved = applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", saved.getId(), "APPLICATION_CREATED", null, toDto(saved));
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Transactional
    public ResponseEntity<Void> deleteApplication(UUID applicationId) {
        Application app = loadApp(applicationId);
        ApplicationDto before = toDto(app);
        app.setStatus("DELETED");
        app.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", applicationId, "APPLICATION_DELETED", before, toDto(app));
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApplicationDto> getApplicationById(UUID applicationId) {
        return ResponseEntity.ok(toDto(loadApp(applicationId)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listApplications(
            Integer page, Integer size, UUID organizationId, String type) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<Application> results = applicationRepository.findFiltered(
                orgId, type, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    @Transactional
    public ResponseEntity<ApplicationCredentialDto> rotateApplicationSecret(UUID applicationId) {
        Application app = loadApp(applicationId);
        String newClientId = UUID.randomUUID().toString();
        app.setClientId(newClientId);
        app.setSecretExpiresAt(OffsetDateTime.now().plusDays(365));
        app.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(app);
        auditService.recordWarning("APPLICATION", applicationId, "APPLICATION_SECRET_ROTATED", null, null);

        // ApplicationCredentialDto v2: applicationId, credentialType, secret, expiresAt
        ApplicationCredentialDto creds = new ApplicationCredentialDto();
        creds.setApplicationId(applicationId);
        creds.setCredentialType(ApplicationCredentialDto.CredentialTypeEnum.fromValue(app.getType()));
        creds.setSecret(newClientId);
        creds.setExpiresAt(app.getSecretExpiresAt());
        return ResponseEntity.ok(creds);
    }

    @Transactional
    public ResponseEntity<ApplicationDto> updateApplication(
            UUID applicationId, UpdateApplicationRequestDto req) {
        Application app = loadApp(applicationId);
        ApplicationDto before = toDto(app);
        if (req.getName() != null)         app.setName(req.getName());
        if (req.getRedirectUris() != null) app.setRedirectUris(req.getRedirectUris());
        if (req.getStatus() != null)       app.setStatus(req.getStatus().getValue());
        app.setUpdatedAt(OffsetDateTime.now());
        Application saved = applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", applicationId, "APPLICATION_UPDATED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    private Application loadApp(UUID applicationId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return applicationRepository.findByOrgIdAndId(orgId, applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        }
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    private ApplicationDto toDto(Application a) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(a.getId());
        dto.setOrganizationId(a.getOrgId());
        dto.setName(a.getName());
        dto.setConnectorType(ApplicationDto.ConnectorTypeEnum.fromValue(a.getType()));
        dto.setClientId(a.getClientId());
        dto.setRedirectUris(a.getRedirectUris());
        dto.setSamlEntityId(a.getSamlEntityId());
        dto.setStatus(ApplicationDto.StatusEnum.fromValue(a.getStatus()));
        if (a.getIntegrationHealthStatus() != null) {
            dto.setIntegrationHealthStatus(
                    ApplicationDto.IntegrationHealthStatusEnum.fromValue(a.getIntegrationHealthStatus()));
        }
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    private PagedResponseDto buildPage(List<?> items, long total, int page, int size) {
        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(items.stream().map(i -> (Object) i).toList());
        dto.setTotalElements(total);
        dto.setNumber(page);
        dto.setSize(size);
        dto.setTotalPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return dto;
    }
}
