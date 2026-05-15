package com.asms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates {@link TenantContext} from the JWT claims on every authenticated request.
 *
 * <p>Expected JWT claims:
 * <ul>
 *   <li>{@code org_id}  — the selected organization's UUID (set after org-selection step)</li>
 *   <li>{@code sub}     — the user's UUID</li>
 *   <li>{@code preferred_username} or {@code username} — the username string</li>
 * </ul>
 *
 * <p>If {@code org_id} is absent (e.g., pre-org-selection token), the context is not set —
 * the first service method that calls {@link TenantContext#getRequiredOrgId()} will throw 403.
 */
@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                String orgIdStr = jwt.getClaimAsString("org_id");
                String subStr   = jwt.getSubject();
                String username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getClaimAsString("username");
                }
                if (orgIdStr != null && subStr != null) {
                    try {
                        UUID orgId  = UUID.fromString(orgIdStr);
                        UUID userId = UUID.fromString(subStr);
                        TenantContext.set(orgId, userId, username);
                        log.trace("TenantContext set — org: {}, user: {}", orgId, userId);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid UUID in JWT claims — org_id: {}, sub: {}", orgIdStr, subStr);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
