package com.asms.service;

import com.asms.domain.User;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateUserRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateUserRequestDto;
import com.asms.model.UserDto;
import com.asms.model.UserStatusUpdateRequestDto;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User lifecycle management service (AC-3, AC-12, AC-13).
 *
 * <p>All list queries are org-scoped via the memberships table (ADR-006 / RISK-002).
 * Every write produces an audit event with before/after state (ADR-009).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public ResponseEntity<UserDto> createUser(CreateUserRequestDto req) {
        log.debug("Create user: {}", req.getUsername());
        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phoneNumber(req.getPhoneNumber())
                .status("PENDING_ACTIVATION")
                .forcePasswordChange(true)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        User saved = userRepository.save(user);
        auditService.recordInfo("USER", saved.getId(), "USER_CREATED", null, toDto(saved));
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Transactional
    public ResponseEntity<Void> deleteUser(UUID userId) {
        User user = loadUser(userId);
        UserDto before = toDto(user);
        user.setStatus("DELETED");
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
        auditService.recordInfo("USER", userId, "USER_DELETED", before, toDto(user));
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<UserDto> getUserById(UUID userId) {
        return ResponseEntity.ok(toDto(loadUser(userId)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listUsers(
            Integer page, Integer size, String sort, String search, UUID organizationId) {
        // AC-13: org scope — use param if provided, otherwise TenantContext
        UUID orgId = organizationId != null ? organizationId : TenantContext.getOrgId();
        Page<User> results = userRepository.findAllByOrgId(
                orgId, search, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    @Transactional
    public ResponseEntity<UserDto> updateUser(UUID userId, UpdateUserRequestDto req) {
        User user = loadUser(userId);
        UserDto before = toDto(user);
        if (req.getFirstName() != null)   user.setFirstName(req.getFirstName());
        if (req.getLastName() != null)    user.setLastName(req.getLastName());
        if (req.getEmail() != null)       user.setEmail(req.getEmail());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        user.setUpdatedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);
        auditService.recordInfo("USER", userId, "USER_UPDATED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    @Transactional
    public ResponseEntity<UserDto> updateUserStatus(UUID userId, UserStatusUpdateRequestDto req) {
        User user = loadUser(userId);
        UserDto before = toDto(user);
        String newStatus = req.getStatus().getValue();
        user.setStatus(newStatus);
        if ("LOCKED".equals(newStatus)) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
        } else if ("ACTIVE".equals(newStatus)) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
        user.setUpdatedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);
        auditService.recordInfo("USER", userId, "USER_STATUS_CHANGED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> !"DELETED".equals(u.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setPhoneNumber(u.getPhoneNumber());
        dto.setStatus(UserDto.StatusEnum.fromValue(u.getStatus()));
        dto.setMfaEnabled(u.isMfaEnabled());
        dto.setFailedLoginAttempts(u.getFailedLoginAttempts());
        dto.setLockedUntil(u.getLockedUntil());
        dto.setLastLoginAt(u.getLastLoginAt());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        return dto;
    }

    private PagedResponseDto buildPage(List<?> items, long total, int page, int size) {
        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(items.stream().map(i -> (Object) i).toList());
        dto.setTotalElements(total);
        dto.setNumber(page);
        dto.setSize(size);
        dto.setTotalPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return dto;
    }
}
