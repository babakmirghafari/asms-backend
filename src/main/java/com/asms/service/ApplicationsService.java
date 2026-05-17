package com.asms.service;

import com.asms.domain.Application;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateApplicationRequestDto;
import com.asms.model.UpdateApplicationRequestDto;
import com.asms.repository.ApplicationRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    /**
     * Result of a secret rotation — carries the new credential data.
     */
    public record RotateSecretResult(UUID applicationId, String credentialType,
                                      String secret, OffsetDateTime expiresAt) {}

    @Transactional
    public Application createApplication(CreateApplicationRequestDto req) {
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
        auditService.recordInfo("APPLICATION", saved.getId(), "APPLICATION_CREATED", null, saved);
        return saved;
    }

    @Transactional
    public void deleteApplication(UUID applicationId) {
        Application app = loadApp(applicationId);
        Application before = cloneForAudit(app);
        app.setStatus("DELETED");
        app.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", applicationId, "APPLICATION_DELETED", before, app);
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(UUID applicationId) {
        return loadApp(applicationId);
    }

    @Transactional(readOnly = true)
    public Page<Application> listApplications(Integer page, Integer size, UUID organizationId, String type) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        return applicationRepository.findFiltered(
                orgId, type, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @Transactional
    public RotateSecretResult rotateApplicationSecret(UUID applicationId) {
        Application app = loadApp(applicationId);
        String newClientId = UUID.randomUUID().toString();
        app.setClientId(newClientId);
        app.setSecretExpiresAt(OffsetDateTime.now().plusDays(365));
        app.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(app);
        auditService.recordWarning("APPLICATION", applicationId, "APPLICATION_SECRET_ROTATED", null, null);
        return new RotateSecretResult(applicationId, app.getType(), newClientId, app.getSecretExpiresAt());
    }

    @Transactional
    public Application updateApplication(UUID applicationId, UpdateApplicationRequestDto req) {
        Application app = loadApp(applicationId);
        Application before = cloneForAudit(app);
        if (req.getName() != null)         app.setName(req.getName());
        if (req.getRedirectUris() != null) app.setRedirectUris(req.getRedirectUris());
        if (req.getStatus() != null)       app.setStatus(req.getStatus().getValue());
        app.setUpdatedAt(OffsetDateTime.now());
        Application saved = applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", applicationId, "APPLICATION_UPDATED", before, saved);
        return saved;
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

    private Application cloneForAudit(Application a) {
        return Application.builder()
                .id(a.getId()).orgId(a.getOrgId()).name(a.getName()).type(a.getType())
                .clientId(a.getClientId()).clientSecretHash(a.getClientSecretHash())
                .redirectUris(a.getRedirectUris()).samlEntityId(a.getSamlEntityId())
                .status(a.getStatus()).integrationHealthStatus(a.getIntegrationHealthStatus())
                .secretExpiresAt(a.getSecretExpiresAt()).createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt())
                .build();
    }
}
