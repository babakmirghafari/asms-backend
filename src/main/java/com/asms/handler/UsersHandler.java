package com.asms.handler;

import com.asms.api.UsersApiDelegate;
import com.asms.model.CreateUserRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateUserRequestDto;
import com.asms.model.UserDto;
import com.asms.model.UserStatusUpdateRequestDto;
import com.asms.service.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the Users API.
 *
 * <p>Implements {@link UsersApiDelegate} — the contract-generated delegation interface.
 * Translates HTTP concerns (DTO in/out, response status) and delegates all business
 * logic to {@link UsersService}.
 *
 * <p>No business logic lives here. This class must remain a thin adapter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsersHandler implements UsersApiDelegate {

    private final UsersService usersService;

    @Override
    public ResponseEntity<UserDto> createUser(CreateUserRequestDto createUserRequestDto) {
        return usersService.createUser(createUserRequestDto);
    }

    @Override
    public ResponseEntity<Void> deleteUser(UUID userId) {
        return usersService.deleteUser(userId);
    }

    @Override
    public ResponseEntity<UserDto> getUserById(UUID userId) {
        return usersService.getUserById(userId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listUsers(
            Integer page, Integer size, String sort, String search, UUID organizationId) {
        return usersService.listUsers(page, size, sort, search, organizationId);
    }

    @Override
    public ResponseEntity<UserDto> updateUser(UUID userId, UpdateUserRequestDto updateUserRequestDto) {
        return usersService.updateUser(userId, updateUserRequestDto);
    }

    @Override
    public ResponseEntity<UserDto> updateUserStatus(UUID userId, UserStatusUpdateRequestDto userStatusUpdateRequestDto) {
        return usersService.updateUserStatus(userId, userStatusUpdateRequestDto);
    }
}
