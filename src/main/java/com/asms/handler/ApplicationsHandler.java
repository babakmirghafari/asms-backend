package com.asms.handler;

import com.asms.api.ApplicationsApiDelegate;
import com.asms.domain.Application;
import com.asms.mapper.ApplicationMapper;
import com.asms.model.ApplicationCredentialDto;
import com.asms.model.ApplicationDto;
import com.asms.model.CreateApplicationRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateApplicationRequestDto;
import com.asms.service.ApplicationsService;
import com.asms.service.ApplicationsService.RotateSecretResult;
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
        Application app = applicationsService.createApplication(createApplicationRequestDto);
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
        Page<Application> apps = applicationsService.listApplications(page, size, organizationId, type);
        List<ApplicationDto> dtos = apps.getContent().stream().map(applicationMapper::toDto).toList();
        return ResponseEntity.ok(buildPage(dtos, apps.getTotalElements(),
                apps.getNumber(), apps.getSize()));
    }

    @Override
    public ResponseEntity<ApplicationCredentialDto> rotateApplicationSecret(UUID applicationId) {
        RotateSecretResult result = applicationsService.rotateApplicationSecret(applicationId);
        ApplicationCredentialDto creds = new ApplicationCredentialDto();
        creds.setApplicationId(result.applicationId());
        creds.setCredentialType(ApplicationCredentialDto.CredentialTypeEnum.fromValue(result.credentialType()));
        creds.setSecret(result.secret());
        creds.setExpiresAt(result.expiresAt());
        return ResponseEntity.ok(creds);
    }

    @Override
    public ResponseEntity<ApplicationDto> updateApplication(
            UUID applicationId, UpdateApplicationRequestDto updateApplicationRequestDto) {
        Application app = applicationsService.updateApplication(applicationId, updateApplicationRequestDto);
        return ResponseEntity.ok(applicationMapper.toDto(app));
    }

    // ─── private helpers ────────────────────────────────────────────────────

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
