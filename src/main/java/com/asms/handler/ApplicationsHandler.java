package com.asms.handler;

import com.asms.api.ApplicationsApiDelegate;
import com.asms.model.ApplicationCredentialDto;
import com.asms.model.ApplicationDto;
import com.asms.model.CreateApplicationRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateApplicationRequestDto;
import com.asms.service.ApplicationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the Applications API.
 *
 * <p>Implements {@link ApplicationsApiDelegate}. Delegates all business logic to {@link ApplicationsService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationsHandler implements ApplicationsApiDelegate {

    private final ApplicationsService applicationsService;

    @Override
    public ResponseEntity<ApplicationDto> createApplication(
            CreateApplicationRequestDto createApplicationRequestDto) {
        return applicationsService.createApplication(createApplicationRequestDto);
    }

    @Override
    public ResponseEntity<Void> deleteApplication(UUID applicationId) {
        return applicationsService.deleteApplication(applicationId);
    }

    @Override
    public ResponseEntity<ApplicationDto> getApplicationById(UUID applicationId) {
        return applicationsService.getApplicationById(applicationId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listApplications(
            Integer page, Integer size, UUID organizationId, String type) {
        return applicationsService.listApplications(page, size, organizationId, type);
    }

    @Override
    public ResponseEntity<ApplicationCredentialDto> rotateApplicationSecret(UUID applicationId) {
        return applicationsService.rotateApplicationSecret(applicationId);
    }

    @Override
    public ResponseEntity<ApplicationDto> updateApplication(
            UUID applicationId, UpdateApplicationRequestDto updateApplicationRequestDto) {
        return applicationsService.updateApplication(applicationId, updateApplicationRequestDto);
    }
}
