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

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StationPolicyMapper {

    @Mapping(target = "organizationId", source = "organization.id")
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

    // Scalar fields only — service must set entity.setOrganization(org) after calling this.
    @Named("toStationPolicyEntity")
    default StationPolicy toStationPolicyEntity(CreateStationPolicyRequestDto dto) {
        if (dto == null) return StationPolicy.builder().build();
        return StationPolicy.builder()
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

    static Short parseTimeString(String time) {
        if (time == null) return null;
        try {
            return Short.parseShort(time.split(":")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
