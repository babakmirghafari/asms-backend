package com.asms.mapper;

import com.asms.domain.Application;
import com.asms.domain.enums.ApplicationStatus;
import com.asms.domain.enums.ConnectorType;
import com.asms.domain.enums.IntegrationHealthStatus;
import com.asms.model.ApplicationDto;
import com.asms.model.CreateApplicationRequestDto;
import com.asms.model.UpdateApplicationRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApplicationMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "connectorType", source = "type")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "integrationHealthStatus", source = "integrationHealthStatus")
    ApplicationDto toDto(Application application);

    @ValueMapping(source = "OIDC", target = "OIDC")
    @ValueMapping(source = "SAML", target = "SAML")
    @ValueMapping(source = "API_TOKEN", target = "API_TOKEN")
    ApplicationDto.ConnectorTypeEnum toConnectorTypeEnum(ConnectorType type);

    @ValueMapping(source = "ACTIVE", target = "ACTIVE")
    @ValueMapping(source = "INACTIVE", target = "INACTIVE")
    @ValueMapping(source = "SUSPENDED", target = "SUSPENDED")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    ApplicationDto.StatusEnum toStatusEnum(ApplicationStatus status);

    @ValueMapping(source = "HEALTHY", target = "HEALTHY")
    @ValueMapping(source = "DEGRADED", target = "DEGRADED")
    @ValueMapping(source = "UNKNOWN", target = "UNKNOWN")
    @ValueMapping(source = "NEVER_CONNECTED", target = "NEVER_CONNECTED")
    ApplicationDto.IntegrationHealthStatusEnum toIntegrationHealthStatusEnum(IntegrationHealthStatus status);

    // Scalar fields only — service must set entity.setOrganization(org) after calling this.
    @Named("toApplicationEntity")
    default Application toApplicationEntity(CreateApplicationRequestDto dto) {
        if (dto == null) return Application.builder().build();
        ConnectorType type = dto.getConnectorType() != null
                ? ConnectorType.valueOf(dto.getConnectorType().getValue())
                : null;
        return Application.builder()
                .name(dto.getName())
                .type(type)
                .redirectUris(dto.getRedirectUris())
                .samlEntityId(dto.getSamlEntityId())
                .status(ApplicationStatus.ACTIVE)
                .integrationHealthStatus(IntegrationHealthStatus.NEVER_CONNECTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Named("toApplicationPatch")
    default Application toApplicationPatch(UpdateApplicationRequestDto dto) {
        if (dto == null) return Application.builder().build();
        ApplicationStatus status = dto.getStatus() != null
                ? ApplicationStatus.valueOf(dto.getStatus().getValue())
                : null;
        return Application.builder()
                .name(dto.getName())
                .redirectUris(dto.getRedirectUris())
                .status(status)
                .build();
    }
}
