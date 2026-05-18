package com.asms.mapper;

import com.asms.domain.StationPolicy;
import com.asms.model.StationPolicyDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper for {@link StationPolicy} domain entity ↔ {@link StationPolicyDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StationPolicyMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    @Mapping(target = "allowedIpRanges", source = "allowedIps")
    @Mapping(target = "allowedDays", source = "allowedDays", qualifiedByName = "shortsToIntegers")
    @Mapping(target = "workStartTime", source = "workHourStart", qualifiedByName = "shortToTimeString")
    @Mapping(target = "workEndTime", source = "workHourEnd", qualifiedByName = "shortToTimeString")
    StationPolicyDto toDto(StationPolicy policy);

    @Named("stringToStatusEnum")
    static StationPolicyDto.StatusEnum stringToStatusEnum(String status) {
        if (status == null) return null;
        try {
            return StationPolicyDto.StatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("shortsToIntegers")
    static List<Integer> shortsToIntegers(List<Short> shorts) {
        if (shorts == null) return null;
        return shorts.stream().map(Short::intValue).toList();
    }

    @Named("shortToTimeString")
    static String shortToTimeString(Short value) {
        return value != null ? value + ":00" : null;
    }
}
