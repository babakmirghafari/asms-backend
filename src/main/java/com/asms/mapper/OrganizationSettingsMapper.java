package com.asms.mapper;

import com.asms.domain.Organization;
import com.asms.domain.OrganizationSettings;
import com.asms.domain.enums.IdentityProvider;
import com.asms.model.OrganizationSettingsDto;
import com.asms.model.UpdateOrganizationSettingsRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.OffsetDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrganizationSettingsMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "identityProvider", source = "identityProvider", qualifiedByName = "domainEnumToDto")
    OrganizationSettingsDto toDto(OrganizationSettings settings);

    /**
     * Creates a new OrganizationSettings entity from an update request DTO and an Organization.
     * Used when no settings row exists yet (initial creation).
     */
    @Named("toEntity")
    default OrganizationSettings toEntity(UpdateOrganizationSettingsRequestDto dto, Organization org) {
        if (dto == null) {
            return OrganizationSettings.builder()
                    .organization(org)
                    .sessionTimeout(30)
                    .maxConcurrentSessions(3)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
        }
        return OrganizationSettings.builder()
                .organization(org)
                .requireMfa(dto.getRequireMfa() != null && dto.getRequireMfa())
                .forceMfaOnSensitive(dto.getForceMfaOnSensitive() != null && dto.getForceMfaOnSensitive())
                .sessionTimeout(dto.getSessionTimeout() != null ? dto.getSessionTimeout() : 30)
                .maxConcurrentSessions(dto.getMaxConcurrentSessions() != null ? dto.getMaxConcurrentSessions() : 3)
                .allowedIpCidrs(dto.getAllowedIpCidrs())
                .ssoEnabled(dto.getSsoEnabled() != null && dto.getSsoEnabled())
                .identityProvider(updateDtoEnumToDomain(dto.getIdentityProvider()))
                .autoProvision(dto.getAutoProvision() != null && dto.getAutoProvision())
                .autoDeprovision(dto.getAutoDeprovision() != null && dto.getAutoDeprovision())
                .primaryColor(dto.getPrimaryColor())
                .customLoginUrl(dto.getCustomLoginUrl())
                .welcomeMessage(dto.getWelcomeMessage())
                .dataResidency(dto.getDataResidency())
                .enforceDataResidencyOnExports(dto.getEnforceDataResidencyOnExports() != null && dto.getEnforceDataResidencyOnExports())
                .longTermAuditRetention(dto.getLongTermAuditRetention() != null && dto.getLongTermAuditRetention())
                .gdprDataExportEndpoint(dto.getGdprDataExportEndpoint() != null && dto.getGdprDataExportEndpoint())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Applies non-null fields from the request DTO to an existing entity (patch semantics).
     * The nullValuePropertyMappingStrategy=IGNORE on the mapper ensures null fields are skipped.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "identityProvider", source = "identityProvider", qualifiedByName = "updateDtoEnumToDomain")
    void patch(UpdateOrganizationSettingsRequestDto dto, @MappingTarget OrganizationSettings settings);

    @Named("domainEnumToDto")
    default OrganizationSettingsDto.IdentityProviderEnum domainEnumToDto(IdentityProvider identityProvider) {
        if (identityProvider == null) return null;
        return OrganizationSettingsDto.IdentityProviderEnum.fromValue(identityProvider.name());
    }

    @Named("updateDtoEnumToDomain")
    default IdentityProvider updateDtoEnumToDomain(UpdateOrganizationSettingsRequestDto.IdentityProviderEnum dtoEnum) {
        if (dtoEnum == null) return null;
        return IdentityProvider.valueOf(dtoEnum.getValue());
    }
}
