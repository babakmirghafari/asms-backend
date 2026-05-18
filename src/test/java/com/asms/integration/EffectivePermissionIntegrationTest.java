package com.asms.integration;

import com.asms.domain.Organization;
import com.asms.domain.Permission;
import com.asms.domain.PermissionGroup;
import com.asms.domain.User;
import com.asms.domain.enums.PermissionStatus;
import com.asms.domain.enums.UserStatus;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.PermissionGroupRepository;
import com.asms.repository.PermissionRepository;
import com.asms.repository.UserRepository;
import com.asms.service.EffectivePermissionService;
import com.asms.service.EffectivePermissionService.EffectivePermissionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AC-11: effective permissions computation.
 *
 * Verifies:
 * - Union of permissions from multiple groups
 * - Deduplication when the same permission appears in two groups
 * - Conflict detection (permission in multiple groups) with correct resolution note
 * - User with no group memberships has zero permissions
 */
@Transactional
class EffectivePermissionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EffectivePermissionService effectivePermissionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionGroupRepository permissionGroupRepository;

    private Organization org;
    private User user;

    @BeforeEach
    void setUp() {
        org = organizationRepository.save(Organization.builder()
                .name("Test Org AC11")
                .slug("test-org-ac11-" + System.nanoTime())
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        user = userRepository.save(User.builder()
                .username("ac11-user-" + System.nanoTime())
                .email("ac11-" + System.nanoTime() + "@test.com")
                .status(UserStatus.ACTIVE)
                .forcePasswordChange(false)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());
    }

    @Test
    @DisplayName("AC-11: user in two groups gets union of both groups' permissions")
    void effectivePermissions_unionOfMultipleGroups() {
        // Given: two permissions and two groups
        Permission permA = permissionRepository.save(Permission.builder()
                .orgId(org.getId())
                .name("perm:read")
                .resource("documents")
                .action("READ")
                .status(PermissionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        Permission permB = permissionRepository.save(Permission.builder()
                .orgId(org.getId())
                .name("perm:write")
                .resource("documents")
                .action("WRITE")
                .status(PermissionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        PermissionGroup groupReaders = permissionGroupRepository.save(PermissionGroup.builder()
                .orgId(org.getId())
                .name("Readers")
                .sensitive(false)
                .permissions(java.util.Set.of(permA))
                .members(java.util.Set.of(user))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        PermissionGroup groupWriters = permissionGroupRepository.save(PermissionGroup.builder()
                .orgId(org.getId())
                .name("Writers")
                .sensitive(false)
                .permissions(java.util.Set.of(permB))
                .members(java.util.Set.of(user))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        // When
        EffectivePermissionResult result = effectivePermissionService.compute(user.getId(), org.getId());

        // Then: both permissions present
        assertThat(result.permissions()).hasSize(2);
        assertThat(result.hasPermission("perm:read")).isTrue();
        assertThat(result.hasPermission("perm:write")).isTrue();
        assertThat(result.groups()).extracting("name")
                .containsExactlyInAnyOrder("Readers", "Writers");
        // No conflicts — each permission comes from exactly one group
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    @DisplayName("AC-11: permission in multiple groups is deduplicated with conflict entry")
    void effectivePermissions_deduplicatesPermissionAcrossGroups_andFlagsConflict() {
        // Given: one permission in two groups, user is member of both
        Permission sharedPerm = permissionRepository.save(Permission.builder()
                .orgId(org.getId())
                .name("perm:admin")
                .resource("system")
                .action("ADMIN")
                .status(PermissionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        PermissionGroup groupA = permissionGroupRepository.save(PermissionGroup.builder()
                .orgId(org.getId())
                .name("GroupA")
                .sensitive(false)
                .permissions(java.util.Set.of(sharedPerm))
                .members(java.util.Set.of(user))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        PermissionGroup groupB = permissionGroupRepository.save(PermissionGroup.builder()
                .orgId(org.getId())
                .name("GroupB")
                .sensitive(false)
                .permissions(java.util.Set.of(sharedPerm))
                .members(java.util.Set.of(user))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        // When
        EffectivePermissionResult result = effectivePermissionService.compute(user.getId(), org.getId());

        // Then: permission appears only once (Set semantics)
        assertThat(result.permissions()).hasSize(1);
        assertThat(result.hasPermission("perm:admin")).isTrue();

        // And: exactly one conflict entry for the duplicated permission
        assertThat(result.conflicts()).hasSize(1);
        assertThat(result.conflicts().get(0).permissionName()).isEqualTo("perm:admin");
        assertThat(result.conflicts().get(0).sources()).hasSize(2);
        assertThat(result.conflicts().get(0).sources())
                .anyMatch(s -> s.contains("GroupA"))
                .anyMatch(s -> s.contains("GroupB"));
    }

    @Test
    @DisplayName("AC-11: user with no group memberships has zero effective permissions")
    void effectivePermissions_noGroups_returnsEmpty() {
        // No group memberships for this user
        EffectivePermissionResult result = effectivePermissionService.compute(user.getId(), org.getId());

        assertThat(result.permissions()).isEmpty();
        assertThat(result.groups()).isEmpty();
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    @DisplayName("AC-11: INACTIVE permissions are excluded from effective set")
    void effectivePermissions_inactivePermissionsExcluded() {
        Permission activePerm = permissionRepository.save(Permission.builder()
                .orgId(org.getId())
                .name("perm:active")
                .resource("resource")
                .action("READ")
                .status(PermissionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        Permission deprecatedPerm = permissionRepository.save(Permission.builder()
                .orgId(org.getId())
                .name("perm:deprecated")
                .resource("resource")
                .action("DELETE")
                .status(PermissionStatus.DEPRECATED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        permissionGroupRepository.save(PermissionGroup.builder()
                .orgId(org.getId())
                .name("MixedGroup")
                .sensitive(false)
                .permissions(java.util.Set.of(activePerm, deprecatedPerm))
                .members(java.util.Set.of(user))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        EffectivePermissionResult result = effectivePermissionService.compute(user.getId(), org.getId());

        // Only the ACTIVE permission appears
        assertThat(result.permissions()).hasSize(1);
        assertThat(result.hasPermission("perm:active")).isTrue();
        assertThat(result.hasPermission("perm:deprecated")).isFalse();
    }
}
