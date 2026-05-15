package com.asms.handler;

import com.asms.api.OrganizationsApiDelegate;
import com.asms.model.CreateOrganizationRequestDto;
import com.asms.model.OrganizationDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateOrganizationRequestDto;
import com.asms.service.OrganizationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the Organizations API.
 *
 * <p>Implements {@link OrganizationsApiDelegate}. Delegates all business logic to {@link OrganizationsService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationsHandler implements OrganizationsApiDelegate {

    private final OrganizationsService organizationsService;

    @Override
    public ResponseEntity<OrganizationDto> createOrganization(
            CreateOrganizationRequestDto createOrganizationRequestDto) {
        return organizationsService.createOrganization(createOrganizationRequestDto);
    }

    @Override
    public ResponseEntity<Void> deleteOrganization(UUID organizationId) {
        return organizationsService.deleteOrganization(organizationId);
    }

    @Override
    public ResponseEntity<OrganizationDto> getOrganizationById(UUID organizationId) {
        return organizationsService.getOrganizationById(organizationId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listOrganizations(Integer page, Integer size, String search) {
        return organizationsService.listOrganizations(page, size, search);
    }

    @Override
    public ResponseEntity<OrganizationDto> updateOrganization(
            UUID organizationId, UpdateOrganizationRequestDto updateOrganizationRequestDto) {
        return organizationsService.updateOrganization(organizationId, updateOrganizationRequestDto);
    }
}
