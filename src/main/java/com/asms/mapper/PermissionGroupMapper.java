package com.asms.mapper;

import com.asms.domain.PermissionGroup;
import com.asms.model.PermissionGroupDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

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
}
