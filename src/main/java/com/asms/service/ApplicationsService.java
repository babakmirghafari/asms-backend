package com.asms.service;

import com.asms.domain.Application;
import com.asms.exception.ResourceNotFoundException;
import com.asms.domain.enums.ApplicationStatus;
import com.asms.domain.enums.ConnectorType;
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

    /**
     * Persists a new application. The handler converts the request DTO to an
     * {@link Application} entity via the mapper before calling here.
     */
    @Transactional
    public Application createApplication(Application app) {
        Application saved = applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", saved.getId(), "APPLICATION_CREATED", null, saved);
        return saved;
    }

    @Transactional
    public void deleteApplication(UUID applicationId) {
        Application app = loadApp(applicationId);
        Application before = cloneForAudit(app);
        app.setStatus(ApplicationStatus.DELETED);
        app.setUpdatedAt(OffsetDateTime.now());
        applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", applicationId, "APPLICATION_DELETED", before, app);
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(UUID applicationId) {
        return loadApp(applicationId);
    }

    @Transactional(readOnly = true)
    public Page<Application> listApplications(Integer page, Integer size, UUID organizationId, ConnectorType type) {
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
        return new RotateSecretResult(applicationId, app.getType().name(), newClientId, app.getSecretExpiresAt());
    }

    /**
     * Applies updates to an existing application.
     * The handler extracts fields from the request DTO before calling here.
     *
     * @param applicationId target application identifier
     * @param patch         partial Application carrying the fields to update (null fields not applied)
     */
    @Transactional
    public Application updateApplication(UUID applicationId, Application patch) {
        Application app = loadApp(applicationId);
        Application before = cloneForAudit(app);
        if (patch.getName() != null)         app.setName(patch.getName());
        if (patch.getRedirectUris() != null) app.setRedirectUris(patch.getRedirectUris());
        if (patch.getStatus() != null)       app.setStatus(patch.getStatus());
        app.setUpdatedAt(OffsetDateTime.now());
        Application saved = applicationRepository.save(app);
        auditService.recordInfo("APPLICATION", applicationId, "APPLICATION_UPDATED", before, saved);
        return saved;
    }

    private Application loadApp(UUID applicationId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return applicationRepository.findByOrganizationIdAndId(orgId, applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        }
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    private Application cloneForAudit(Application a) {
        return Application.builder()
                .id(a.getId()).organization(a.getOrganization()).name(a.getName()).type(a.getType())
                .clientId(a.getClientId()).clientSecretHash(a.getClientSecretHash())
                .redirectUris(a.getRedirectUris()).samlEntityId(a.getSamlEntityId())
                .status(a.getStatus()).integrationHealthStatus(a.getIntegrationHealthStatus())
                .secretExpiresAt(a.getSecretExpiresAt()).createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt())
                .build();
    }
}
