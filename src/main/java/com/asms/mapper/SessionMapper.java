package com.asms.mapper;

import com.asms.domain.Session;
import com.asms.domain.enums.SessionStatus;
import com.asms.model.SessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

/**
 * MapStruct mapper for {@link Session} domain entity ↔ {@link SessionDto} contract DTO.
 *
 * <p>SessionStatus → SessionDto.StatusEnum: ACTIVE, EXPIRED, REVOKED names match directly.
 * Explicit {@code @ValueMapping} method is provided per the enum conversion pattern.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "riskScore", source = "riskScore", qualifiedByName = "shortToFloat")
    // lastActivityAt approximated by createdAt (domain has no dedicated field)
    @Mapping(target = "lastActivityAt", source = "createdAt")
    SessionDto toDto(Session session);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    SessionDto.StatusEnum toStatusEnum(SessionStatus status);

    @Named("shortToFloat")
    static Float shortToFloat(short value) {
        return (float) value;
    }
}
