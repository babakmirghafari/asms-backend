package com.asms.handler;

import com.asms.api.ApplicationsApiDelegate;
import com.asms.domain.Application;
import com.asms.domain.enums.ConnectorType;
import com.asms.mapper.ApplicationMapper;
import com.asms.model.ApplicationCredentialDto;
import com.asms.model.ApplicationDto;
import com.asms.model.CreateApplicationRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateApplicationRequestDto;
import com.asms.security.TenantContext;
import com.asms.service.ApplicationsService;
import com.asms.service.ApplicationsService.RotateSecretResult;
import com.asms.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Applications API.
 *
 * <p>Implements {@link ApplicationsApiDelegate}. Delegates all business logic to
 * {@link ApplicationsService}. Maps domain {@link Application} ↔ {@link ApplicationDto}
 * via {@link ApplicationMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationsHandler implements ApplicationsApiDelegate {

    private final ApplicationsService applicationsService;
    private final ApplicationMapper applicationMapper;

    @Override
    public ResponseEntity<ApplicationDto> createApplication(
            CreateApplicationRequestDto createApplicationRequestDto) {
        Application entity = applicationMapper.toApplicationEntity(createApplicationRequestDto);
        UUID orgId = createApplicationRequestDto.getOrganizationId() != null
                ? createApplicationRequestDto.getOrganizationId()
                : TenantContext.getRequiredOrgId();
        Application app = applicationsService.createApplication(entity, orgId);
        return ResponseEntity.status(201).body(applicationMapper.toDto(app));
    }

    @Override
    public ResponseEntity<Void> deleteApplication(UUID applicationId) {
        applicationsService.deleteApplication(applicationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApplicationDto> getApplicationById(UUID applicationId) {
        return ResponseEntity.ok(applicationMapper.toDto(applicationsService.getApplicationById(applicationId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listApplications(
            Integer page, Integer size, UUID organizationId, String type) {
        ConnectorType connectorType = type != null ? ConnectorType.valueOf(type) : null;
        Page<Application> apps = applicationsService.listApplications(page, size, organizationId, connectorType);
        List<ApplicationDto> dtos = apps.getContent().stream().map(applicationMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, apps));
    }

    @Override
    public ResponseEntity<ApplicationCredentialDto> rotateApplicationSecret(UUID applicationId) {
        RotateSecretResult result = applicationsService.rotateApplicationSecret(applicationId);
        ApplicationCredentialDto creds = new ApplicationCredentialDto();
        creds.setApplicationId(result.applicationId());
        // credentialType from service is the enum name (e.g. "OIDC") — map to CredentialTypeEnum
        try {
            creds.setCredentialType(ApplicationCredentialDto.CredentialTypeEnum.fromValue(result.credentialType()));
        } catch (IllegalArgumentException e) {
            creds.setCredentialType(ApplicationCredentialDto.CredentialTypeEnum.CLIENT_SECRET);
        }
        creds.setSecret(result.secret());
        creds.setExpiresAt(result.expiresAt());
        return ResponseEntity.ok(creds);
    }

    @Override
    public ResponseEntity<ApplicationDto> updateApplication(
            UUID applicationId, UpdateApplicationRequestDto updateApplicationRequestDto) {
        Application patch = applicationMapper.toApplicationPatch(updateApplicationRequestDto);
        Application app = applicationsService.updateApplication(applicationId, patch);
        return ResponseEntity.ok(applicationMapper.toDto(app));
    }

}
