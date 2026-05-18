package com.asms.mapper;

import com.asms.domain.Alert;
import com.asms.model.AlertDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.math.BigDecimal;

/**
 * MapStruct mapper for {@link Alert} domain entity ↔ {@link AlertDto} contract DTO.
 *
 * <p>AlertDto (v2.0.0) exposes: id, alertType, severity, status, riskScore, riskLevel,
 * title, description, actorId, actorUsername, organizationId, ipAddress,
 * acknowledgedBy, acknowledgedAt, acknowledgeNote, createdAt.
 *
 * <p>AlertSeverity and AlertStatus are mapped by enum name — names match AlertDto enums directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlertMapper {

    @Mapping(target = "alertType", source = "type")
    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "actorId", source = "userId")
    @Mapping(target = "severity", source = "severity")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "riskScore", source = "riskScore", qualifiedByName = "bigDecimalToFloat")
    @Mapping(target = "riskLevel", source = "riskLevel", qualifiedByName = "stringToRiskLevelEnum")
    @Mapping(target = "actorUsername", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "acknowledgeNote", ignore = true)
    AlertDto toDto(Alert alert);

    @Named("bigDecimalToFloat")
    static Float bigDecimalToFloat(BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }

    @Named("stringToRiskLevelEnum")
    static AlertDto.RiskLevelEnum stringToRiskLevelEnum(String riskLevel) {
        if (riskLevel == null) return null;
        try {
            return AlertDto.RiskLevelEnum.fromValue(riskLevel);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
