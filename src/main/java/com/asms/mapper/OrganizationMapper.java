package com.asms.mapper;

import com.asms.domain.Organization;
import com.asms.domain.enums.OrganizationStatus;
import com.asms.model.CreateOrganizationRequestDto;
import com.asms.model.OrganizationDto;
import com.asms.model.UpdateOrganizationRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.OffsetDateTime;

/**
 * MapStruct mapper for {@link Organization} domain entity ↔ {@link OrganizationDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 *
 * <p>OrganizationStatus → OrganizationDto.StatusEnum uses {@code @ValueMapping}.
 * DELETED is mapped to NULL — deleted organizations are excluded from the API surface.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    @Mapping(target = "parentOrganizationId", source = "parentOrgId")
    @Mapping(target = "status", source = "status")
    OrganizationDto toDto(Organization org);

    @ValueMapping(source = "ACTIVE", target = "ACTIVE")
    @ValueMapping(source = "SUSPENDED", target = "SUSPENDED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    OrganizationDto.StatusEnum toStatusEnum(OrganizationStatus status);

    /**
     * Converts a {@link CreateOrganizationRequestDto} to a new {@link Organization} entity.
     * Slug generation is delegated to the handler prior to this call since slug is a domain concern.
     */
    @Named("toOrganizationEntity")
    default Organization toOrganizationEntity(CreateOrganizationRequestDto dto, String slug) {
        if (dto == null) return Organization.builder().build();
        return Organization.builder()
                .name(dto.getName())
                .slug(slug)
                .parentOrgId(dto.getParentOrganizationId())
                .logoUrl(dto.getLogoUrl())
                .status(OrganizationStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Converts an {@link UpdateOrganizationRequestDto} to a partial {@link Organization} patch.
     * Only non-null DTO fields are populated; the service applies them selectively.
     */
    @Named("toOrganizationPatch")
    default Organization toOrganizationPatch(UpdateOrganizationRequestDto dto) {
        if (dto == null) return Organization.builder().build();
        OrganizationStatus status = dto.getStatus() != null
                ? OrganizationStatus.valueOf(dto.getStatus().getValue())
                : null;
        return Organization.builder()
                .name(dto.getName())
                .logoUrl(dto.getLogoUrl())
                .status(status)
                .build();
    }
}
