package com.asms.service;

import com.asms.domain.Permission;
import com.asms.domain.PermissionGroup;
import com.asms.domain.User;
import com.asms.domain.enums.PermissionStatus;
import com.asms.domain.enums.UserStatus;
import com.asms.exception.ResourceNotFoundException;
import com.asms.repository.PermissionGroupRepository;
import com.asms.repository.UserRepository;
import com.asms.service.EffectivePermissionService.EffectivePermissionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link EffectivePermissionService}.
 *
 * <p>All repository interactions are stubbed via Mockito.
 * Tests cover the three scenarios mandated by the task:
 * <ol>
 *   <li>User with two groups → effective perms = union of both groups' permissions</li>
 *   <li>User with one group + one direct perm → effective perms includes both</li>
 *   <li>Overlapping group perm and direct perm → no duplicates in result</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class EffectivePermissionServiceTest {

    @Mock
    private PermissionGroupRepository permissionGroupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EffectivePermissionService effectivePermissionService;

    private UUID userId;
    private UUID orgId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId  = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("test-user")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .forcePasswordChange(false)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .directPermissions(new HashSet<>())
                .build();
    }

    // ─── helper factories ────────────────────────────────────────────────────

    private Permission activePermission(String name) {
        return Permission.builder()
                .id(UUID.randomUUID())
                .name(name)
                .resource("res")
                .action("ACTION")
                .status(PermissionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private Permission deprecatedPermission(String name) {
        return Permission.builder()
                .id(UUID.randomUUID())
                .name(name)
                .resource("res")
                .action("ACTION")
                .status(PermissionStatus.DEPRECATED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private PermissionGroup groupWith(String name, Set<Permission> permissions) {
        return PermissionGroup.builder()
                .id(UUID.randomUUID())
                .name(name)
                .sensitive(false)
                .permissions(permissions)
                .members(Set.of(user))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ─── scenario 1: two groups → union ─────────────────────────────────────

    @Test
    @DisplayName("User in two groups gets the union of both groups' permissions")
    void compute_userInTwoGroups_returnsUnion() {
        Permission permRead  = activePermission("perm:read");
        Permission permWrite = activePermission("perm:write");

        PermissionGroup groupA = groupWith("Readers", Set.of(permRead));
        PermissionGroup groupB = groupWith("Writers", Set.of(permWrite));

        given(permissionGroupRepository.findGroupsForUser(userId, orgId))
                .willReturn(List.of(groupA, groupB));
        given(userRepository.findByIdWithGroupsAndPermissions(userId))
                .willReturn(Optional.of(user)); // no direct permissions

        EffectivePermissionResult result = effectivePermissionService.compute(userId, orgId);

        assertThat(result.permissions()).hasSize(2);
        assertThat(result.hasPermission("perm:read")).isTrue();
        assertThat(result.hasPermission("perm:write")).isTrue();
        assertThat(result.groups()).extracting("name")
                .containsExactlyInAnyOrder("Readers", "Writers");
        assertThat(result.conflicts()).isEmpty();
    }

    // ─── scenario 2: one group + one direct perm ─────────────────────────────

    @Test
    @DisplayName("User with one group and one direct permission gets both in effective set")
    void compute_groupPlusDirectPermission_includesBoth() {
        Permission groupPerm  = activePermission("perm:group");
        Permission directPerm = activePermission("perm:direct");

        PermissionGroup group = groupWith("SomeGroup", Set.of(groupPerm));
        user.getDirectPermissions().add(directPerm);

        given(permissionGroupRepository.findGroupsForUser(userId, orgId))
                .willReturn(List.of(group));
        given(userRepository.findByIdWithGroupsAndPermissions(userId))
                .willReturn(Optional.of(user));

        EffectivePermissionResult result = effectivePermissionService.compute(userId, orgId);

        assertThat(result.permissions()).hasSize(2);
        assertThat(result.hasPermission("perm:group")).isTrue();
        assertThat(result.hasPermission("perm:direct")).isTrue();
    }

    // ─── scenario 3: overlapping group perm and direct perm → no duplicates ─

    @Test
    @DisplayName("Permission present in group and as direct assignment appears only once")
    void compute_overlappingGroupAndDirectPermission_noDuplicates() {
        Permission sharedPerm = activePermission("perm:shared");

        PermissionGroup group = groupWith("SomeGroup", Set.of(sharedPerm));
        user.getDirectPermissions().add(sharedPerm); // same permission object

        given(permissionGroupRepository.findGroupsForUser(userId, orgId))
                .willReturn(List.of(group));
        given(userRepository.findByIdWithGroupsAndPermissions(userId))
                .willReturn(Optional.of(user));

        EffectivePermissionResult result = effectivePermissionService.compute(userId, orgId);

        assertThat(result.permissions()).hasSize(1);
        assertThat(result.hasPermission("perm:shared")).isTrue();
        // Sources should list both "group:SomeGroup" and "direct"
        assertThat(result.permissionSources().get(sharedPerm.getId()))
                .containsExactlyInAnyOrder("group:SomeGroup", "direct");
    }

    // ─── additional: user not found → ResourceNotFoundException ─────────────

    @Test
    @DisplayName("User not found during compute throws ResourceNotFoundException")
    void compute_userNotFound_throwsResourceNotFoundException() {
        given(permissionGroupRepository.findGroupsForUser(userId, orgId))
                .willReturn(List.of());
        given(userRepository.findByIdWithGroupsAndPermissions(userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> effectivePermissionService.compute(userId, orgId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── additional: deprecated direct permission is excluded ────────────────

    @Test
    @DisplayName("Deprecated direct permission is excluded from effective set")
    void compute_deprecatedDirectPermission_excluded() {
        Permission activePerm     = activePermission("perm:active");
        Permission deprecatedPerm = deprecatedPermission("perm:deprecated");

        PermissionGroup group = groupWith("Group", Set.of(activePerm));
        user.getDirectPermissions().add(deprecatedPerm);

        given(permissionGroupRepository.findGroupsForUser(userId, orgId))
                .willReturn(List.of(group));
        given(userRepository.findByIdWithGroupsAndPermissions(userId))
                .willReturn(Optional.of(user));

        EffectivePermissionResult result = effectivePermissionService.compute(userId, orgId);

        assertThat(result.permissions()).hasSize(1);
        assertThat(result.hasPermission("perm:active")).isTrue();
        assertThat(result.hasPermission("perm:deprecated")).isFalse();
    }

    // ─── additional: no groups, no direct perms → empty ──────────────────────

    @Test
    @DisplayName("User with no groups and no direct permissions has empty effective set")
    void compute_noGroupsNoDirect_returnsEmpty() {
        given(permissionGroupRepository.findGroupsForUser(userId, orgId))
                .willReturn(List.of());
        given(userRepository.findByIdWithGroupsAndPermissions(userId))
                .willReturn(Optional.of(user)); // directPermissions is empty

        EffectivePermissionResult result = effectivePermissionService.compute(userId, orgId);

        assertThat(result.permissions()).isEmpty();
        assertThat(result.groups()).isEmpty();
        assertThat(result.conflicts()).isEmpty();
    }
}
