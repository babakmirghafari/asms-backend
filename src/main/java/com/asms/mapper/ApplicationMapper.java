package com.asms.mapper;

import com.asms.domain.Application;
import com.asms.model.ApplicationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link Application} domain entity ↔ {@link ApplicationDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "connectorType", source = "type", qualifiedByName = "stringToConnectorTypeEnum")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    @Mapping(target = "integrationHealthStatus", source = "integrationHealthStatus", qualifiedByName = "stringToHealthStatusEnum")
    ApplicationDto toDto(Application application);

    @Named("stringToConnectorTypeEnum")
    static ApplicationDto.ConnectorTypeEnum stringToConnectorTypeEnum(String type) {
        if (type == null) return null;
        try {
            return ApplicationDto.ConnectorTypeEnum.fromValue(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("stringToStatusEnum")
    static ApplicationDto.StatusEnum stringToStatusEnum(String status) {
        if (status == null) return null;
        try {
            return ApplicationDto.StatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("stringToHealthStatusEnum")
    static ApplicationDto.IntegrationHealthStatusEnum stringToHealthStatusEnum(String status) {
        if (status == null) return null;
        try {
            return ApplicationDto.IntegrationHealthStatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
