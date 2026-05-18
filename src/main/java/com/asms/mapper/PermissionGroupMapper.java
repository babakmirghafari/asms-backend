package com.asms.mapper;

import com.asms.domain.PermissionGroup;
import com.asms.model.CreatePermissionGroupRequestDto;
import com.asms.model.PermissionGroupDto;
import com.asms.security.TenantContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.time.OffsetDateTime;

/**
 * MapStruct mapper for {@link PermissionGroup} domain entity ↔ {@link PermissionGroupDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionGroupMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "memberCount", expression = "java(group.getMembers() != null ? group.getMembers().size() : 0)")
    @Mapping(target = "permissionCount", expression = "java(group.getPermissions() != null ? group.getPermissions().size() : 0)")
    PermissionGroupDto toDto(PermissionGroup group);

    /**
     * Converts a {@link CreatePermissionGroupRequestDto} to a new {@link PermissionGroup} entity.
     * The org ID is sourced from {@link TenantContext} at call time.
     */
    @Named("toPermissionGroupEntity")
    default PermissionGroup toPermissionGroupEntity(CreatePermissionGroupRequestDto dto) {
        if (dto == null) return PermissionGroup.builder().build();
        return PermissionGroup.builder()
                .orgId(TenantContext.getRequiredOrgId())
                .name(dto.getName())
                .description(dto.getDescription())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
