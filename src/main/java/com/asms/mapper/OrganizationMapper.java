package com.asms.mapper;

import com.asms.domain.Organization;
import com.asms.model.OrganizationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link Organization} domain entity ↔ {@link OrganizationDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    @Mapping(target = "parentOrganizationId", source = "parentOrgId")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    OrganizationDto toDto(Organization org);

    @Named("stringToStatusEnum")
    static OrganizationDto.StatusEnum stringToStatusEnum(String status) {
        if (status == null) return null;
        try {
            return OrganizationDto.StatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
