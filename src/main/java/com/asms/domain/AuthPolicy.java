package com.asms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false, unique = true)
    private UUID orgId;

    @Column(name = "max_failed_attempts", nullable = false)
    private int maxFailedAttempts;

    @Column(name = "lockout_duration_minutes", nullable = false)
    private int lockoutDurationMinutes;

    @Column(name = "require_mfa", nullable = false)
    private boolean requireMfa;

    @Column(name = "password_min_length", nullable = false)
    private int passwordMinLength;

    @Column(name = "password_require_uppercase", nullable = false)
    private boolean passwordRequireUppercase;

    @Column(name = "password_require_numbers", nullable = false)
    private boolean passwordRequireNumbers;

    @Column(name = "password_require_symbols", nullable = false)
    private boolean passwordRequireSymbols;

    @Column(name = "password_history_count", nullable = false)
    private int passwordHistoryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
