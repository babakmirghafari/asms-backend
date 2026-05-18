package com.asms.mapper;

import com.asms.domain.Session;
import com.asms.model.SessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link Session} domain entity ↔ {@link SessionDto} contract DTO.
 *
 * <p>SessionStatus maps by name — ACTIVE, EXPIRED, REVOKED match SessionDto.StatusEnum directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "riskScore", source = "riskScore", qualifiedByName = "shortToFloat")
    // lastActivityAt approximated by createdAt (domain has no dedicated field)
    @Mapping(target = "lastActivityAt", source = "createdAt")
    SessionDto toDto(Session session);

    @Named("shortToFloat")
    static Float shortToFloat(short value) {
        return (float) value;
    }
}
