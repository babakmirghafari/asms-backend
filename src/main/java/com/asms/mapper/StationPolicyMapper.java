package com.asms.mapper;

import com.asms.domain.StationPolicy;
import com.asms.domain.enums.StationPolicyStatus;
import com.asms.model.CreateStationPolicyRequestDto;
import com.asms.model.StationPolicyDto;
import com.asms.model.UpdateStationPolicyRequestDto;
import com.asms.security.TenantContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * MapStruct mapper for {@link StationPolicy} domain entity ↔ {@link StationPolicyDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 *
 * <p>StationPolicyStatus → StationPolicyDto.StatusEnum uses {@code @ValueMapping}.
 * Time fields (workHourStart/End) remain as {@code @Named} helpers since they are
 * format conversions (Short → "HH:00" string), not enum-to-enum mappings.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StationPolicyMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "allowedIpRanges", source = "allowedIps")
    @Mapping(target = "allowedDays", source = "allowedDays", qualifiedByName = "shortsToIntegers")
    @Mapping(target = "workStartTime", source = "workHourStart", qualifiedByName = "shortToTimeString")
    @Mapping(target = "workEndTime", source = "workHourEnd", qualifiedByName = "shortToTimeString")
    StationPolicyDto toDto(StationPolicy policy);

    @ValueMapping(source = "ACTIVE", target = "ACTIVE")
    @ValueMapping(source = "INACTIVE", target = "INACTIVE")
    StationPolicyDto.StatusEnum toStatusEnum(StationPolicyStatus status);

    @Named("shortsToIntegers")
    static List<Integer> shortsToIntegers(List<Short> shorts) {
        if (shorts == null) return null;
        return shorts.stream().map(Short::intValue).toList();
    }

    @Named("shortToTimeString")
    static String shortToTimeString(Short value) {
        return value != null ? value + ":00" : null;
    }

    /**
     * Converts a {@link CreateStationPolicyRequestDto} to a new {@link StationPolicy} entity.
     * The org ID is sourced from the DTO if present, otherwise from {@link TenantContext}.
     */
    @Named("toStationPolicyEntity")
    default StationPolicy toStationPolicyEntity(CreateStationPolicyRequestDto dto) {
        if (dto == null) return StationPolicy.builder().build();
        java.util.UUID orgId = dto.getOrganizationId() != null
                ? dto.getOrganizationId()
                : TenantContext.getRequiredOrgId();
        return StationPolicy.builder()
                .orgId(orgId)
                .userId(null) // v2: policies are org-scoped, no userId
                .name(dto.getName())
                .description(dto.getDescription())
                .status(StationPolicyStatus.ACTIVE)
                .allowedIps(dto.getAllowedIpRanges())
                .allowedDays(dto.getAllowedDays() != null
                        ? dto.getAllowedDays().stream().map(Integer::shortValue).toList()
                        : null)
                .workHourStart(parseTimeString(dto.getWorkStartTime()))
                .workHourEnd(parseTimeString(dto.getWorkEndTime()))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Converts an {@link UpdateStationPolicyRequestDto} to a partial {@link StationPolicy} patch.
     * Only non-null DTO fields are populated; the service applies them selectively.
     */
    @Named("toStationPolicyPatch")
    default StationPolicy toStationPolicyPatch(UpdateStationPolicyRequestDto dto) {
        if (dto == null) return StationPolicy.builder().build();
        StationPolicyStatus status = dto.getStatus() != null
                ? StationPolicyStatus.valueOf(dto.getStatus().getValue())
                : null;
        return StationPolicy.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .status(status)
                .allowedIps(dto.getAllowedIpRanges())
                .allowedDays(dto.getAllowedDays() != null
                        ? dto.getAllowedDays().stream().map(Integer::shortValue).toList()
                        : null)
                .workHourStart(parseTimeString(dto.getWorkStartTime()))
                .workHourEnd(parseTimeString(dto.getWorkEndTime()))
                .build();
    }

    /** Parses an HH:mm or HH:mm:ss time string to the hour component as Short. */
    static Short parseTimeString(String time) {
        if (time == null) return null;
        try {
            return Short.parseShort(time.split(":")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
