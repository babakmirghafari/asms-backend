package com.asms.mapper;

import com.asms.domain.Permission;
import com.asms.domain.enums.PermissionStatus;
import com.asms.model.CreatePermissionRequestDto;
import com.asms.model.PermissionDto;
import com.asms.model.PermissionSummaryDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "action", source = "action", qualifiedByName = "stringToActionEnum")
    @Mapping(target = "status", source = "status")
    PermissionDto toDto(Permission permission);

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "action", source = "action", qualifiedByName = "stringToSummaryActionEnum")
    PermissionSummaryDto toSummaryDto(Permission permission);

    @Named("stringToSummaryActionEnum")
    static PermissionSummaryDto.ActionEnum stringToSummaryActionEnum(String action) {
        if (action == null) return null;
        try {
            return PermissionSummaryDto.ActionEnum.fromValue(action);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    PermissionDto.StatusEnum toStatusEnum(PermissionStatus status);

    @Named("stringToActionEnum")
    static PermissionDto.ActionEnum stringToActionEnum(String action) {
        if (action == null) return null;
        try {
            return PermissionDto.ActionEnum.fromValue(action);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Scalar fields only — service must set entity.setOrganization(org) after calling this.
    // action is kept as raw String (sourced from CSV imports) — intentional exception to enum pattern.
    @Named("toPermissionEntity")
    default Permission toPermissionEntity(CreatePermissionRequestDto dto) {
        if (dto == null) return Permission.builder().build();
        return Permission.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .resource(dto.getResource() != null ? dto.getResource() : dto.getName().split("\\.")[0])
                .action(dto.getAction() != null ? dto.getAction().getValue() : "READ")
                .status(PermissionStatus.DRAFT)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
