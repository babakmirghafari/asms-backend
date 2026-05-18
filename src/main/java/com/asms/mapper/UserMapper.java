package com.asms.mapper;

import com.asms.domain.User;
import com.asms.domain.enums.UserStatus;
import com.asms.model.CreateUserRequestDto;
import com.asms.model.UserDto;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link User} domain entity ↔ {@link UserDto} contract DTO.
 *
 * <p>Generated implementation is a Spring component — inject via {@code @Autowired}
 * or constructor injection. Handlers use this mapper; services never touch DTOs directly.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "status", source = "status")
    UserDto toDto(User user);

    @ValueMapping(source = "PENDING_ACTIVATION", target = MappingConstants.NULL)
    @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
    UserDto.StatusEnum toStatusEnum(UserStatus status);

    @Mapping(qualifiedByName = "toUserEntity")
    User toUserEntityFromCreateUserRequestDto(CreateUserRequestDto createUserRequestDto);

    @Named("toUserEntity")
    default User toUser(CreateUserRequestDto  dto) {
        // TODO body of Mapping

        return User.builder().build();
    }
}
