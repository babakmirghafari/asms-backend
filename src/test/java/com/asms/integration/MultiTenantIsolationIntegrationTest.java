package com.asms.integration;

import com.asms.domain.Membership;
import com.asms.domain.Organization;
import com.asms.domain.Permission;
import com.asms.domain.User;
import com.asms.domain.enums.AuditSeverity;
import com.asms.domain.enums.MembershipStatus;
import com.asms.domain.enums.OrganizationStatus;
import com.asms.domain.enums.PermissionStatus;
import com.asms.domain.enums.UserRole;
import com.asms.domain.enums.UserStatus;
import com.asms.exception.AccessDeniedException;
import com.asms.repository.AuditLogRepository;
import com.asms.repository.AuditLogSpecifications;
import com.asms.repository.MembershipRepository;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.PermissionRepository;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import com.asms.service.UsersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for AC-13: multi-tenant isolation.
 *
 * Verifies:
 * - User listing is scoped to the org in TenantContext (org-A users not returned for org-B)
 * - Permission queries are scoped to org_id (org-A permissions not returned for org-B)
 * - Audit log queries return only records belonging to the org in context
 * - Missing org context causes AccessDeniedException (maps to 403 in the API layer)
 * - Direct repository queries with org_id correctly filter cross-tenant data
 */
@Transactional
class MultiTenantIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UsersService usersService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Organization orgA;
    private Organization orgB;
    private User userInOrgA;
    private User userInOrgB;

    @BeforeEach
    void setUp() {
        long nonce = System.nanoTime();

        orgA = organizationRepository.save(Organization.builder()
                .name("Org A AC13")
                .slug("org-a-ac13-" + nonce)
                .status(OrganizationStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        orgB = organizationRepository.save(Organization.builder()
                .name("Org B AC13")
                .slug("org-b-ac13-" + nonce)
                .status(OrganizationStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        userInOrgA = userRepository.save(User.builder()
                .username("user-orga-" + nonce)
                .email("user-orga-" + nonce + "@test.com")
                .status(UserStatus.ACTIVE)
                .forcePasswordChange(false)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        userInOrgB = userRepository.save(User.builder()
                .username("user-orgb-" + nonce)
                .email("user-orgb-" + nonce + "@test.com")
                .status(UserStatus.ACTIVE)
                .forcePasswordChange(false)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        // Assign userInOrgA to orgA only
        membershipRepository.save(Membership.builder()
                .user(userInOrgA)
                .organization(orgA)
                .role(UserRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        // Assign userInOrgB to orgB only
        membershipRepository.save(Membership.builder()
                .user(userInOrgB)
                .organization(orgB)
                .role(UserRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("AC-13: listUsers with orgA context returns only orgA members")
    void listUsers_orgAContext_returnsOnlyOrgAMembers() {
        TenantContext.set(orgA.getId(), userInOrgA.getId(), userInOrgA.getUsername());

        Page<User> page = usersService.listUsers(0, 20, null, null, orgA.getId());

        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(userInOrgA.getId());
    }

    @Test
    @DisplayName("AC-13: listUsers with orgB context does not expose orgA members")
    void listUsers_orgBContext_doesNotExposeOrgAMembers() {
        TenantContext.set(orgB.getId(), userInOrgB.getId(), userInOrgB.getUsername());

        Page<User> page = usersService.listUsers(0, 20, null, null, orgB.getId());

        assertThat(page).isNotNull();
        // Confirm orgA's user is not in the results
        assertThat(page.getContent()).noneMatch(u -> u.getId().equals(userInOrgA.getId()));
    }

    @Test
    @DisplayName("AC-13: permission repository query is scoped by org_id")
    void permissionQuery_orgScopedByOrgId_doesNotCrossOrgs() {
        // Create a permission in orgA
        Permission orgAPermission = permissionRepository.save(Permission.builder()
                .organization(orgA)
                .name("orga:only:perm")
                .resource("documents")
                .action("READ")
                .status(PermissionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        // Query permissions for orgA — must return the permission
        Page<Permission> orgAResults = permissionRepository.findByOrganizationId(
                orgA.getId(), PageRequest.of(0, 20));
        assertThat(orgAResults.getContent()).anyMatch(p -> p.getId().equals(orgAPermission.getId()));

        // Query permissions for orgB — must not return orgA's permission
        Page<Permission> orgBResults = permissionRepository.findByOrganizationId(
                orgB.getId(), PageRequest.of(0, 20));
        assertThat(orgBResults.getContent()).noneMatch(p -> p.getId().equals(orgAPermission.getId()));
    }

    @Test
    @DisplayName("AC-13: getRequiredOrgId with no context throws AccessDeniedException (maps to 403)")
    void tenantContext_missingOrgId_throwsAccessDeniedException() {
        // Ensure no context is set
        TenantContext.clear();

        assertThatThrownBy(TenantContext::getRequiredOrgId)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No organization context");
    }

    @Test
    @DisplayName("AC-13: direct permission fetch with wrong org_id returns empty")
    void permissionFetch_wrongOrgId_returnsEmpty() {
        Permission orgAPermission = permissionRepository.save(Permission.builder()
                .organization(orgA)
                .name("orga:secret:perm")
                .resource("secrets")
                .action("READ")
                .status(PermissionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        // Fetching with orgB's ID should return empty — isolation enforced at repository level
        assertThat(permissionRepository.findByOrganizationIdAndId(orgB.getId(), orgAPermission.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("AC-13: audit log query scoped to requesting org returns only that org's entries")
    void auditLogQuery_orgScoped_doesNotCrossOrgs() {
        // Insert audit logs directly for orgA and orgB
        com.asms.domain.AuditLog orgALog = auditLogRepository.save(com.asms.domain.AuditLog.builder()
                .organization(orgA)
                .actor(userInOrgA)
                .actorUsername(userInOrgA.getUsername())
                .targetType("USER")
                .action("USER_CREATED")
                .severity(AuditSeverity.INFO)
                .previousHash("0".repeat(64))
                .entryHash("a".repeat(64))
                .createdAt(OffsetDateTime.now())
                .build());

        com.asms.domain.AuditLog orgBLog = auditLogRepository.save(com.asms.domain.AuditLog.builder()
                .organization(orgB)
                .actor(userInOrgB)
                .actorUsername(userInOrgB.getUsername())
                .targetType("USER")
                .action("USER_CREATED")
                .severity(AuditSeverity.INFO)
                .previousHash("0".repeat(64))
                .entryHash("b".repeat(64))
                .createdAt(OffsetDateTime.now())
                .build());

        // Query for orgA — must not include orgB's entry
        // Using JpaSpecificationExecutor to avoid PostgreSQL "cannot determine data type" on null params
        Page<com.asms.domain.AuditLog> orgAResults = auditLogRepository.findAll(
                AuditLogSpecifications.belongsToOrg(orgA.getId()),
                PageRequest.of(0, 20));
        assertThat(orgAResults.getContent()).anyMatch(l -> l.getId().equals(orgALog.getId()));
        assertThat(orgAResults.getContent()).noneMatch(l -> l.getId().equals(orgBLog.getId()));

        // Query for orgB — must not include orgA's entry
        Page<com.asms.domain.AuditLog> orgBResults = auditLogRepository.findAll(
                AuditLogSpecifications.belongsToOrg(orgB.getId()),
                PageRequest.of(0, 20));
        assertThat(orgBResults.getContent()).anyMatch(l -> l.getId().equals(orgBLog.getId()));
        assertThat(orgBResults.getContent()).noneMatch(l -> l.getId().equals(orgALog.getId()));
    }
}
