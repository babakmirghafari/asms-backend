package com.asms.mapper;

import com.asms.domain.Alert;
import com.asms.model.AlertDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

/**
 * MapStruct mapper for {@link Alert} domain entity ↔ {@link AlertDto} contract DTO.
 *
 * <p>AlertDto (v2.0.0) exposes: id, alertType, severity, status, riskScore, riskLevel,
 * title, description, actorId, actorUsername, organizationId, ipAddress,
 * acknowledgedBy, acknowledgedAt, acknowledgeNote, createdAt.
 */
@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(target = "alertType", source = "type")
    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "actorId", source = "userId")
    @Mapping(target = "severity", source = "severity", qualifiedByName = "stringToSeverityEnum")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    @Mapping(target = "riskScore", source = "riskScore", qualifiedByName = "bigDecimalToFloat")
    @Mapping(target = "riskLevel", source = "riskLevel", qualifiedByName = "stringToRiskLevelEnum")
    @Mapping(target = "actorUsername", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "acknowledgeNote", ignore = true)
    AlertDto toDto(Alert alert);

    @Named("stringToSeverityEnum")
    static AlertDto.SeverityEnum stringToSeverityEnum(String severity) {
        if (severity == null) return null;
        try {
            return AlertDto.SeverityEnum.fromValue(severity);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("stringToStatusEnum")
    static AlertDto.StatusEnum stringToStatusEnum(String status) {
        if (status == null) return null;
        try {
            return AlertDto.StatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

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
