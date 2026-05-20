package com.asms.handler;

import com.asms.api.OrganizationsApiDelegate;
import com.asms.domain.Organization;
import com.asms.mapper.OrganizationMapper;
import com.asms.model.CreateOrganizationRequestDto;
import com.asms.model.OrganizationDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateOrganizationRequestDto;
import com.asms.service.OrganizationsService;
import com.asms.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Organizations API.
 *
 * <p>Implements {@link OrganizationsApiDelegate}. Delegates all business logic to
 * {@link OrganizationsService}. Maps domain {@link Organization} ↔ {@link OrganizationDto}
 * via {@link OrganizationMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationsHandler implements OrganizationsApiDelegate {

    private final OrganizationsService organizationsService;
    private final OrganizationMapper organizationMapper;

    @Override
    public ResponseEntity<OrganizationDto> createOrganization(
            CreateOrganizationRequestDto createOrganizationRequestDto) {
        String slug = generateSlug(createOrganizationRequestDto.getName());
        Organization entity = organizationMapper.toOrganizationEntity(
                createOrganizationRequestDto, slug);
        UUID parentId = createOrganizationRequestDto.getParentOrganizationId();
        Organization org = organizationsService.createOrganization(entity, parentId);
        return ResponseEntity.status(201).body(organizationMapper.toDto(org));
    }

    @Override
    public ResponseEntity<Void> deleteOrganization(UUID organizationId) {
        organizationsService.deleteOrganization(organizationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<OrganizationDto> getOrganizationById(UUID organizationId) {
        return ResponseEntity.ok(organizationMapper.toDto(organizationsService.getOrganizationById(organizationId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listOrganizations(Integer page, Integer size, String search) {
        Page<Organization> orgs = organizationsService.listOrganizations(page, size, search);
        List<OrganizationDto> dtos = orgs.getContent().stream().map(organizationMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, orgs));
    }

    @Override
    public ResponseEntity<OrganizationDto> updateOrganization(
            UUID organizationId, UpdateOrganizationRequestDto updateOrganizationRequestDto) {
        Organization patch = organizationMapper.toOrganizationPatch(updateOrganizationRequestDto);
        Organization org = organizationsService.updateOrganization(organizationId, patch);
        return ResponseEntity.ok(organizationMapper.toDto(org));
    }

    /** Mirrors the slug generation logic from OrganizationsService (now moved here). */
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

}
