package com.asms.service;

import com.asms.constant.AuditActions;
import com.asms.domain.Membership;
import com.asms.domain.Organization;
import com.asms.domain.User;
import com.asms.domain.enums.DeliveryMethod;
import com.asms.domain.enums.MembershipStatus;
import com.asms.domain.enums.UserRole;
import com.asms.domain.enums.UserStatus;
import com.asms.model.CreateUserRequestDto;
import com.asms.repository.MembershipRepository;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.PermissionGroupRepository;
import com.asms.repository.PermissionRepository;
import com.asms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link UsersService#createUser(CreateUserRequestDto)}.
 *
 * <p>Covers: base field mapping, status/timestamp defaults, membership creation
 * for each organizationId, skipping unknown org IDs, and audit event emission.
 */
@ExtendWith(MockitoExtension.class)
class UsersServiceCreateUserTest {

    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private PermissionGroupRepository permissionGroupRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private UsersService usersService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .status(UserStatus.PENDING_ACTIVATION)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        given(userRepository.save(any(User.class))).willReturn(savedUser);
    }

    // ─── base field mapping ─────────────────────────────────────────────────

    @Test
    @DisplayName("createUser maps base fields from DTO onto the persisted User")
    void createUser_mapsBaseFields() {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("alice");
        dto.setFullName("Alice Smith");
        dto.setEmail("alice@example.com");
        dto.setPhoneNumber("+1-555-0100");
        dto.setSendTempPassword(true);

        User result = usersService.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();

        assertThat(persisted.getUsername()).isEqualTo("alice");
        assertThat(persisted.getFullName()).isEqualTo("Alice Smith");
        assertThat(persisted.getEmail()).isEqualTo("alice@example.com");
        assertThat(persisted.getPhoneNumber()).isEqualTo("+1-555-0100");
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(result).isSameAs(savedUser);
    }

    // ─── delivery method mapping ─────────────────────────────────────────────

    @Test
    @DisplayName("sendTempPassword=true maps to DeliveryMethod.Email")
    void createUser_sendTempPasswordTrue_setsEmailDelivery() {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("bob");
        dto.setEmail("bob@example.com");
        dto.setSendTempPassword(true);

        usersService.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryMethod()).isEqualTo(DeliveryMethod.Email);
    }

    @Test
    @DisplayName("sendTempPassword=false maps to DeliveryMethod.Manuel_Copy")
    void createUser_sendTempPasswordFalse_setsManuelCopyDelivery() {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("carol");
        dto.setEmail("carol@example.com");
        dto.setSendTempPassword(false);

        usersService.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryMethod()).isEqualTo(DeliveryMethod.Manuel_Copy);
    }

    @Test
    @DisplayName("sendTempPassword=null maps to DeliveryMethod.Manuel_Copy")
    void createUser_sendTempPasswordNull_setsManuelCopyDelivery() {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("dave");
        dto.setEmail("dave@example.com");
        dto.setSendTempPassword(null);

        usersService.createUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getDeliveryMethod()).isEqualTo(DeliveryMethod.Manuel_Copy);
    }

    // ─── membership creation ─────────────────────────────────────────────────

    @Test
    @DisplayName("Creating a user with 2 organizationIds results in 2 Membership records saved with ACTIVE status")
    void createUser_withTwoOrgIds_savesTwoActiveMemberships() {
        UUID orgId1 = UUID.randomUUID();
        UUID orgId2 = UUID.randomUUID();

        Organization org1 = Organization.builder().id(orgId1).name("Org1")
                .slug("org1").build();
        Organization org2 = Organization.builder().id(orgId2).name("Org2")
                .slug("org2").build();

        given(organizationRepository.findById(orgId1)).willReturn(Optional.of(org1));
        given(organizationRepository.findById(orgId2)).willReturn(Optional.of(org2));

        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("eve");
        dto.setEmail("eve@example.com");
        dto.setOrganizationIds(List.of(orgId1, orgId2));

        usersService.createUser(dto);

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository, times(2)).save(membershipCaptor.capture());

        List<Membership> memberships = membershipCaptor.getAllValues();
        assertThat(memberships).hasSize(2);
        assertThat(memberships).allSatisfy(m -> {
            assertThat(m.getUser()).isSameAs(savedUser);
            assertThat(m.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
            assertThat(m.getRole()).isEqualTo(UserRole.MEMBER);
            assertThat(m.getCreatedAt()).isNotNull();
            assertThat(m.getUpdatedAt()).isNotNull();
        });
        assertThat(memberships).extracting(m -> m.getOrganization().getId())
                .containsExactlyInAnyOrder(orgId1, orgId2);
    }

    @Test
    @DisplayName("Unknown organizationId is skipped — no Membership saved for it")
    void createUser_unknownOrgId_skipsAndContinues() {
        UUID knownOrgId   = UUID.randomUUID();
        UUID unknownOrgId = UUID.randomUUID();

        Organization knownOrg = Organization.builder().id(knownOrgId).name("Known")
                .slug("known").build();

        given(organizationRepository.findById(knownOrgId)).willReturn(Optional.of(knownOrg));
        given(organizationRepository.findById(unknownOrgId)).willReturn(Optional.empty());

        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("frank");
        dto.setEmail("frank@example.com");
        dto.setOrganizationIds(List.of(knownOrgId, unknownOrgId));

        usersService.createUser(dto);

        // Only one membership saved (for the known org)
        verify(membershipRepository, times(1)).save(any(Membership.class));
    }

    @Test
    @DisplayName("Null organizationIds list causes no Membership saves")
    void createUser_nullOrgIds_noMembershipSaved() {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("grace");
        dto.setEmail("grace@example.com");
        dto.setOrganizationIds(null);

        usersService.createUser(dto);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("Empty organizationIds list causes no Membership saves")
    void createUser_emptyOrgIds_noMembershipSaved() {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("henry");
        dto.setEmail("henry@example.com");
        dto.setOrganizationIds(List.of());

        usersService.createUser(dto);

        verify(membershipRepository, never()).save(any());
    }

    // ─── audit event ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser always records a USER_CREATED audit event")
    void createUser_recordsAuditEvent() {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setUsername("ivan");
        dto.setEmail("ivan@example.com");

        usersService.createUser(dto);

        verify(auditService).recordInfo(
                "USER", savedUser.getId(), AuditActions.USER_CREATED, null, savedUser);
    }
}
