package com.asms.handler;

import com.asms.api.OrganizationsApiDelegate;
import com.asms.domain.Organization;
import com.asms.model.CreateOrganizationRequestDto;
import com.asms.model.OrganizationDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateOrganizationRequestDto;
import com.asms.service.OrganizationsService;
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
 * {@link OrganizationsService}. Maps domain {@link Organization} to {@link OrganizationDto}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationsHandler implements OrganizationsApiDelegate {

    private final OrganizationsService organizationsService;

    @Override
    public ResponseEntity<OrganizationDto> createOrganization(
            CreateOrganizationRequestDto createOrganizationRequestDto) {
        Organization org = organizationsService.createOrganization(createOrganizationRequestDto);
        return ResponseEntity.status(201).body(toDto(org));
    }

    @Override
    public ResponseEntity<Void> deleteOrganization(UUID organizationId) {
        organizationsService.deleteOrganization(organizationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<OrganizationDto> getOrganizationById(UUID organizationId) {
        return ResponseEntity.ok(toDto(organizationsService.getOrganizationById(organizationId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listOrganizations(Integer page, Integer size, String search) {
        Page<Organization> orgs = organizationsService.listOrganizations(page, size, search);
        List<OrganizationDto> dtos = orgs.getContent().stream().map(this::toDto).toList();
        return ResponseEntity.ok(buildPage(dtos, orgs.getTotalElements(),
                orgs.getNumber(), orgs.getSize()));
    }

    @Override
    public ResponseEntity<OrganizationDto> updateOrganization(
            UUID organizationId, UpdateOrganizationRequestDto updateOrganizationRequestDto) {
        Organization org = organizationsService.updateOrganization(
                organizationId, updateOrganizationRequestDto);
        return ResponseEntity.ok(toDto(org));
    }

    // ─── private helpers ────────────────────────────────────────────────────

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
