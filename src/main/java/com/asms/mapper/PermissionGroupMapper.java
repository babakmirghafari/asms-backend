package com.asms.mapper;

import com.asms.domain.PermissionGroup;
import com.asms.model.CreatePermissionGroupRequestDto;
import com.asms.model.PermissionGroupDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.time.OffsetDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PermissionGroupMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "memberCount", expression = "java(group.getMembers() != null ? group.getMembers().size() : 0)")
    @Mapping(target = "permissionCount", expression = "java(group.getPermissions() != null ? group.getPermissions().size() : 0)")
    PermissionGroupDto toDto(PermissionGroup group);

    // Scalar fields only — service must set entity.setOrganization(org) after calling this.
    @Named("toPermissionGroupEntity")
    default PermissionGroup toPermissionGroupEntity(CreatePermissionGroupRequestDto dto) {
        if (dto == null) return PermissionGroup.builder().build();
        return PermissionGroup.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
