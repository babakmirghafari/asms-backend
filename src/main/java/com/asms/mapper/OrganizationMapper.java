package com.asms.mapper;

import com.asms.domain.Organization;
import com.asms.domain.enums.OrganizationPlan;
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

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    @Mapping(target = "parentOrganizationId", source = "parentOrganization.id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "plan", source = "plan")
    OrganizationDto toDto(Organization org);

    @ValueMapping(source = "ACTIVE", target = "ACTIVE")
    @ValueMapping(source = "SUSPENDED", target = "SUSPENDED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    OrganizationDto.StatusEnum toStatusEnum(OrganizationStatus status);

    @ValueMapping(source = "STARTER", target = "STARTER")
    @ValueMapping(source = "PROFESSIONAL", target = "PROFESSIONAL")
    @ValueMapping(source = "ENTERPRISE", target = "ENTERPRISE")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    OrganizationDto.PlanEnum toPlanEnum(OrganizationPlan plan);

    // Scalar fields only — if dto.getParentOrganizationId() is non-null, service must
    // load the parent Organization and call entity.setParentOrganization(parent).
    @Named("toOrganizationEntity")
    default Organization toOrganizationEntity(CreateOrganizationRequestDto dto, String slug) {
        if (dto == null) return Organization.builder().build();
        return Organization.builder()
                .name(dto.getName())
                .slug(slug)
                .domain(dto.getDomain())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .status(OrganizationStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Named("toOrganizationPatch")
    default Organization toOrganizationPatch(UpdateOrganizationRequestDto dto) {
        if (dto == null) return Organization.builder().build();
        OrganizationStatus status = dto.getStatus() != null
                ? OrganizationStatus.valueOf(dto.getStatus().getValue())
                : null;
        return Organization.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .status(status)
                .build();
    }
}
