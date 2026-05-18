package com.asms.mapper;

import com.asms.domain.Permission;
import com.asms.domain.enums.PermissionStatus;
import com.asms.model.PermissionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

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
}
