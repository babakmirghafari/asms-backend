package com.asms.security;

import java.util.UUID;

/**
 * Thread-local holder for the current request's organization context.
 *
 * <p>Set by {@link TenantContextFilter} on every authenticated request.
 * All service-layer queries use {@link #getRequiredOrgId()} to enforce
 * org-scoped isolation (ADR-006, AC-13).
 *
 * <p>Always call {@link #clear()} in a finally block after use to prevent leakage
 * across virtual-thread hand-offs — done automatically by TenantContextFilter.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_ORG = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID orgId, UUID userId, String username) {
        CURRENT_ORG.set(orgId);
        CURRENT_USER.set(userId);
        CURRENT_USERNAME.set(username);
    }

    public static UUID getOrgId() {
        return CURRENT_ORG.get();
    }

    public static UUID getUserId() {
        return CURRENT_USER.get();
    }

    public static String getUsername() {
        return CURRENT_USERNAME.get();
    }

    /**
     * Returns the current org ID, throwing if not set.
     * Use this in all service methods that require an org context.
     *
     * @throws com.asms.exception.AccessDeniedException if called outside a tenant context
     */
    public static UUID getRequiredOrgId() {
        UUID orgId = CURRENT_ORG.get();
        if (orgId == null) {
            throw new com.asms.exception.AccessDeniedException("No organization context established for this request");
        }
        return orgId;
    }

    public static void clear() {
        CURRENT_ORG.remove();
        CURRENT_USER.remove();
        CURRENT_USERNAME.remove();
    }
}
