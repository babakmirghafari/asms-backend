package com.asms.mapper;

import com.asms.domain.Permission;
import com.asms.domain.enums.PermissionStatus;
import com.asms.model.CreatePermissionRequestDto;
import com.asms.model.PermissionDto;
import com.asms.security.TenantContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MapStruct mapper for {@link Permission} domain entity ↔ {@link PermissionDto} contract DTO.
 *
 * <p>PermissionStatus → PermissionDto.StatusEnum: DRAFT, ACTIVE, DEPRECATED names match directly.
 * Explicit {@code @ValueMapping} method is provided per the enum conversion pattern.
 *
 * <p>The {@code action} field is a raw {@code String} in the domain (sourced from CSV imports and
 * direct inserts) and is converted to {@code PermissionDto.ActionEnum} via {@code fromValue()}.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "action", source = "action", qualifiedByName = "stringToActionEnum")
    @Mapping(target = "status", source = "status")
    PermissionDto toDto(Permission permission);

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

    /**
     * Converts a {@link CreatePermissionRequestDto} to a new {@link Permission} entity.
     * The org ID is sourced from TenantContext, falling back to the DTO's organizationId.
     *
     * <p>The {@code action} field is kept as a raw String (sourced from CSV imports and
     * direct inserts) — intentional documented exception to the domain enum pattern.
     */
    @Named("toPermissionEntity")
    default Permission toPermissionEntity(CreatePermissionRequestDto dto) {
        if (dto == null) return Permission.builder().build();
        UUID orgId = TenantContext.getOrgId();
        if (orgId == null) orgId = dto.getOrganizationId();
        return Permission.builder()
                .orgId(orgId)
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
