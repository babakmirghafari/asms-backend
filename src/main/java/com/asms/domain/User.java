package com.asms.domain;

import com.asms.domain.enums.DeliveryMethod;
import com.asms.domain.enums.Department;
import com.asms.domain.enums.TemporaryPasswordExpiry;
import com.asms.domain.enums.UserStatus;
import com.asms.domain.enums.converter.DeliveryMethodConverter;
import com.asms.domain.enums.converter.DepartmentConverter;
import com.asms.domain.enums.converter.UserStatusConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "department")
    @Convert(converter = DepartmentConverter.class)
    private Department department;

    @Column(name = "manager")
    private String manager;

    @Column(name = "status", nullable = false)
    @Convert(converter = UserStatusConverter.class)
    private UserStatus status;

    @Column(name = "temporary_password_hash")
    private String  temporaryPasswordHash;

    @Column(name = "temporary_password_expiry")
    private TemporaryPasswordExpiry temporaryPasswordExpiry;

    @Column(name = "delivery_method", nullable = false)
    @Convert(converter = DeliveryMethodConverter.class)
    @Builder.Default
    private DeliveryMethod deliveryMethod = DeliveryMethod.Email;


    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_secret_encrypted")
    private String mfaSecretEncrypted;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Membership> memberships;

    /**
     * Inverse side of the {@code permission_group_members} join table.
     * The owning side is {@link PermissionGroup#members} — no new table needed.
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PermissionGroup> permissionGroups = new HashSet<>();

    /**
     * Direct permission assignments outside of groups, backed by the
     * {@code user_permissions} join table (V3 migration).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_permissions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @Builder.Default
    private Set<Permission> directPermissions = new HashSet<>();
}
