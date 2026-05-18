package com.asms.handler;

import com.asms.api.UsersApiDelegate;
import com.asms.domain.User;
import com.asms.domain.enums.UserStatus;
import com.asms.mapper.UserMapper;
import com.asms.model.CreateUserRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateUserRequestDto;
import com.asms.model.UserDto;
import com.asms.model.UserStatusUpdateRequestDto;
import com.asms.service.UsersService;
import com.asms.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Users API.
 *
 * <p>Implements {@link UsersApiDelegate} — the contract-generated delegation interface.
 * Translates HTTP concerns (DTO in/out, response status) and delegates all business
 * logic to {@link UsersService}.
 *
 * <p>No business logic lives here. Entity → DTO conversion is done by
 * {@link UserMapper}. This class must remain a thin adapter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsersHandler implements UsersApiDelegate {

    private final UsersService usersService;
    private final UserMapper userMapper;

    @Override
    public ResponseEntity<UserDto> createUser(CreateUserRequestDto createUserRequestDto) {
        User user = userMapper.toUserEntityFromCreateUserRequestDto(createUserRequestDto);
        usersService.createUser(user);
        return ResponseEntity.status(201).body(userMapper.toDto(user));
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID userId) {
        usersService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserDto> getUserById(UUID userId) {
        return ResponseEntity.ok(userMapper.toDto(usersService.getUserById(userId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listUsers(
            Integer page, Integer size, String sort, String search, UUID organizationId) {
        Page<User> users = usersService.listUsers(page, size, sort, search, organizationId);
        List<UserDto> dtos = users.getContent().stream().map(userMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, users));
    }

    @Override
    public ResponseEntity<UserDto> updateUser(UUID userId, UpdateUserRequestDto updateUserRequestDto) {
        User patch = userMapper.toUserPatch(updateUserRequestDto);
        User updated = usersService.updateUser(userId, patch);
        return ResponseEntity.ok(userMapper.toDto(updated));
    }

    @Override
    public ResponseEntity<UserDto> updateUserStatus(UUID userId,
            UserStatusUpdateRequestDto userStatusUpdateRequestDto) {
        UserStatus newStatus = UserStatus.valueOf(userStatusUpdateRequestDto.getStatus().getValue());
        User updated = usersService.updateUserStatus(userId, newStatus);
        return ResponseEntity.ok(userMapper.toDto(updated));
    }

}
