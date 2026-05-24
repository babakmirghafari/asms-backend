package com.asms.mapper;

import com.asms.domain.Permission;
import com.asms.domain.User;
import com.asms.domain.enums.Department;
import com.asms.domain.enums.UserStatus;
import com.asms.model.CreateUserRequestDto;
import com.asms.model.UpdateUserRequestDto;
import com.asms.model.UserDto;
import com.asms.model.UserDtoDirectPermissionsInner;
import org.hibernate.Hibernate;
import org.mapstruct.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "status", source = "status")
    @Mapping(target = "displayName", ignore = true)
    @Mapping(target = "organizationIds", ignore = true)
    @Mapping(target = "workdays", ignore = true)
    @Mapping(target = "ipRestriction", ignore = true)
    @Mapping(target = "workHours", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "directPermissions", ignore = true)
    UserDto toDto(User user);

    @AfterMapping
    default void fillDirectPermissions(User user, @MappingTarget UserDto dto) {
        if (!Hibernate.isInitialized(user.getDirectPermissions())) return;
        List<UserDtoDirectPermissionsInner> items = user.getDirectPermissions().stream()
                .map(p -> {
                    UserDtoDirectPermissionsInner inner = new UserDtoDirectPermissionsInner();
                    inner.setId(p.getId());
                    inner.setName(p.getName());
                    inner.setResource(p.getResource());
                    if (p.getAction() != null) {
                        try {
                            inner.setAction(UserDtoDirectPermissionsInner.ActionEnum.fromValue(p.getAction()));
                        } catch (IllegalArgumentException ignored) { }
                    }
                    if (p.getOrganization() != null) {
                        inner.setOrganizationId(p.getOrganization().getId());
                    }
                    return inner;
                })
                .toList();
        dto.setDirectPermissions(items);
    }

    // PENDING_ACTIVATION is a server-side pre-activation state — exposed as TEMP_PASSWORD to the UI
    // DELETED users are filtered out before mapping, but a null fallback avoids NPE in edge cases
    @ValueMapping(source = "PENDING_ACTIVATION", target = "TEMP_PASSWORD")
    @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
    UserDto.StatusEnum toStatusEnum(UserStatus status);

    default User toUserEntityFromCreateUserRequestDto(CreateUserRequestDto dto) {
        return toUser(dto);
    }

    @Named("toUserEntity")
    default User toUser(CreateUserRequestDto dto) {
        if (dto == null) return User.builder().build();
        return User.builder()
                .username(dto.getUsername())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .department(dto.getDepartment() != null ? Department.valueOf(dto.getDepartment()) : null)
                .build();
    }

    @Named("toUserPatch")
    default User toUserPatch(UpdateUserRequestDto dto) {
        if (dto == null) return User.builder().build();
        String fullName = Stream.of(dto.getFirstName(), dto.getLastName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .trim();
        return User.builder()
                .fullName(fullName.isEmpty() ? null : fullName)
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }
}
