package com.asms.config;

import com.asms.domain.User;
import com.asms.domain.enums.UserStatus;
import com.asms.repository.MembershipRepository;
import com.asms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * SuperAdmin bootstrap — runs once on application startup.
 *
 * <p>If no user with role {@link UserRole#SUPER_ADMIN} exists in the DB, generates
 * a random secure password, creates the superadmin user, and prints the plain-text
 * password to the console with a highly visible WARNING banner.
 *
 * <p>The superadmin is created with {@code force_password_change = true} so the next
 * login triggers the password-change flow.
 *
 * <p>This bootstrap is safe to run on every startup — it is a no-op if a SUPER_ADMIN
 * already exists.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap {

    private static final String SUPER_ADMIN_USERNAME = "superadmin";
    private static final String SUPER_ADMIN_EMAIL    = "superadmin@asms.local";

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        // Check if a SUPER_ADMIN user already exists (by username convention)
        boolean superAdminExists = userRepository.existsByUsername(SUPER_ADMIN_USERNAME);
        if (superAdminExists) {
            log.debug("SuperAdmin user already exists — skipping bootstrap");
            return;
        }

        // Generate a cryptographically random password (24 chars base64 URL-safe)
        byte[] randomBytes = new byte[18];
        new SecureRandom().nextBytes(randomBytes);
        String plainPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String hashedPassword = passwordEncoder.encode(plainPassword);

        User superAdmin = User.builder()
                .username(SUPER_ADMIN_USERNAME)
                .email(SUPER_ADMIN_EMAIL)
                .firstName("Super")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .passwordHash(hashedPassword)
                .forcePasswordChange(true)   // must change password on first login
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        userRepository.save(superAdmin);

        // Print the plain-text password with a banner that cannot be missed
        log.warn("╔══════════════════════════════════════════════════════════════╗");
        log.warn("║          ASMS SUPERADMIN ACCOUNT CREATED (FIRST RUN)        ║");
        log.warn("╠══════════════════════════════════════════════════════════════╣");
        log.warn("║  Username : {}                                    ║", SUPER_ADMIN_USERNAME);
        log.warn("║  Password : {}                               ║", plainPassword);
        log.warn("║  IMPORTANT: Change this password immediately after first login ║");
        log.warn("╚══════════════════════════════════════════════════════════════╝");
    }
}
