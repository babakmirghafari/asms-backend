package com.asms.mapper;

import com.asms.domain.Alert;
import com.asms.domain.enums.AlertRiskLevel;
import com.asms.domain.enums.AlertSeverity;
import com.asms.domain.enums.AlertStatus;
import com.asms.model.AlertDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlertMapper {

    @Mapping(target = "alertType", source = "type")
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "actorId", source = "user.id")
    @Mapping(target = "severity", source = "severity")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "riskScore", source = "riskScore", qualifiedByName = "bigDecimalToFloat")
    @Mapping(target = "riskLevel", source = "riskLevel")
    @Mapping(target = "actorUsername", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "acknowledgeNote", ignore = true)
    AlertDto toDto(Alert alert);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    AlertDto.SeverityEnum toSeverityEnum(AlertSeverity severity);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    AlertDto.StatusEnum toStatusEnum(AlertStatus status);

    @ValueMapping(source = "LOW", target = "LOW")
    @ValueMapping(source = "MEDIUM", target = "MEDIUM")
    @ValueMapping(source = "HIGH", target = "HIGH")
    @ValueMapping(source = "CRITICAL", target = "CRITICAL")
    AlertDto.RiskLevelEnum toRiskLevelEnum(AlertRiskLevel riskLevel);

    @Named("bigDecimalToFloat")
    static Float bigDecimalToFloat(BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }
}
