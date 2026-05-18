package com.asms.mapper;

import com.asms.domain.Membership;
import com.asms.domain.enums.MembershipStatus;
import com.asms.domain.enums.UserRole;
import com.asms.model.CreateMembershipRequestDto;
import com.asms.model.MembershipDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import java.time.OffsetDateTime;

/**
 * MapStruct mapper for {@link Membership} domain entity ↔ {@link MembershipDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via constructor injection.
 * Handlers use this mapper; services never touch DTOs directly.
 *
 * <p>MembershipStatus domain values vs MembershipDto.StatusEnum contract values:
 * PENDING     → NULL  (no contract equivalent — membership not yet confirmed)
 * ACTIVE      → ACTIVE
 * SUSPENDED   → SUSPENDED
 * REMOVED     → NULL  (no contract equivalent — terminal state, excluded from API surface)
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MembershipMapper {

    @Mapping(target = "organizationId", source = "orgId")
    @Mapping(target = "status", source = "status")
    MembershipDto toDto(Membership membership);

    @ValueMapping(source = "PENDING", target = MappingConstants.NULL)
    @ValueMapping(source = "REMOVED", target = MappingConstants.NULL)
    MembershipDto.StatusEnum toStatusEnum(MembershipStatus status);

    /**
     * Converts a {@link CreateMembershipRequestDto} to a new {@link Membership} domain entity.
     * Defaults role to MEMBER and status to ACTIVE per business rules.
     */
    @Named("toMembershipEntity")
    default Membership toMembershipEntity(CreateMembershipRequestDto dto) {
        if (dto == null) return Membership.builder().build();
        return Membership.builder()
                .userId(dto.getUserId())
                .orgId(dto.getOrganizationId())
                .role(UserRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
