package com.asms.mapper;

import com.asms.domain.Session;
import com.asms.domain.enums.SessionStatus;
import com.asms.model.SessionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "riskScore", source = "riskScore", qualifiedByName = "shortToFloat")
    @Mapping(target = "lastActivityAt", source = "createdAt")
    @Mapping(target = "mfaVerified", ignore = true)
    SessionDto toDto(Session session);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
    SessionDto.StatusEnum toStatusEnum(SessionStatus status);

    @Named("shortToFloat")
    static Float shortToFloat(short value) {
        return (float) value;
    }
}
