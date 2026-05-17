package com.asms.mapper;

import com.asms.domain.Session;
import com.asms.model.SessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link Session} domain entity ↔ {@link SessionDto} contract DTO.
 */
@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatusEnum")
    @Mapping(target = "riskScore", source = "riskScore", qualifiedByName = "shortToFloat")
    // lastActivityAt approximated by createdAt (domain has no dedicated field)
    @Mapping(target = "lastActivityAt", source = "createdAt")
    SessionDto toDto(Session session);

    @Named("stringToStatusEnum")
    static SessionDto.StatusEnum stringToStatusEnum(String status) {
        if (status == null) return null;
        try {
            return SessionDto.StatusEnum.fromValue(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("shortToFloat")
    static Float shortToFloat(short value) {
        return (float) value;
    }
}
