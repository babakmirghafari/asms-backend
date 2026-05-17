package com.asms.mapper;

import com.asms.domain.Permission;
import com.asms.model.PermissionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link Permission} domain entity ↔ {@link PermissionDto} contract DTO.
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "action", source = "action", qualifiedByName = "stringToActionEnum")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    PermissionDto toDto(Permission permission);

    @Named("stringToActionEnum")
    static PermissionDto.ActionEnum stringToActionEnum(String action) {
        if (action == null) return null;
        try {
            return PermissionDto.ActionEnum.fromValue(action);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("stringToStatusEnum")
    static PermissionDto.StatusEnum stringToStatusEnum(String status) {
        if (status == null) return null;
        try {
            return PermissionDto.StatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
