package com.asms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Validates the JWT Bearer token on every protected request.
 *
 * <p>When a valid token is found:
 * <ol>
 *   <li>Parses claims from the token (userId, orgId, username, roles).</li>
 *   <li>Populates {@link SecurityContext} with a {@link UsernamePasswordAuthenticationToken}.</li>
 *   <li>Populates {@link TenantContext} so services can read orgId/userId directly —
 *       no re-parsing of the token downstream.</li>
 * </ol>
 *
 * <p>Public endpoints (login, health, actuator) are excluded via Spring Security's
 * permit-all rules, so this filter will simply find no Bearer token and continue
 * without setting any context.
 *
 * <p>{@link TenantContext#clear()} is always called in a finally block to prevent
 * leakage across virtual-thread hand-offs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractBearerToken(request);
            if (StringUtils.hasText(token)) {
                processToken(token);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private void processToken(String token) {
        try {
            Claims claims = jwtService.parseToken(token);

            String userIdStr = claims.getSubject();
            String username  = claims.get("username", String.class);
            String orgIdStr  = claims.get("org_id", String.class);

            if (userIdStr == null) {
                log.warn("JWT missing 'sub' claim — skipping TenantContext setup");
                return;
            }

            UUID userId = UUID.fromString(userIdStr);
            UUID orgId  = orgIdStr != null ? UUID.fromString(orgIdStr) : null;

            // Populate TenantContext for downstream services
            TenantContext.set(orgId, userId, username);
            log.trace("TenantContext set — org: {}, user: {}", orgId, userId);

            // Populate Spring SecurityContext so @PreAuthorize and other security
            // constructs recognise the request as authenticated
            List<?> rawRoles = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = rawRoles == null
                    ? List.of()
                    : rawRoles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toString()))
                            .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            // Do not set context — security filter chain will reject if endpoint is protected
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}
