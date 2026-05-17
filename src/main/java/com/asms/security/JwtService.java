package com.asms.security;

import com.asms.config.AsmsSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT sign / verify / parse component.
 *
 * <p>Uses {@code io.jsonwebtoken:jjwt-api} 0.12.x. The signing key is derived
 * from {@code asms.security.jwt.secret} — must be at least 32 characters.
 * Inject this service instead of raw JJWT calls anywhere in the codebase.
 *
 * <p>JWT payload claims:
 * <ul>
 *   <li>{@code sub}       — userId (UUID)</li>
 *   <li>{@code username}  — username string</li>
 *   <li>{@code org_id}    — primary organisation UUID</li>
 *   <li>{@code org_slug}  — organisation slug</li>
 *   <li>{@code roles}     — list of role strings</li>
 *   <li>{@code exp}       — expiry timestamp (set automatically)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtService {

    private final AsmsSecurityProperties securityProperties;

    /**
     * Creates and signs a JWT token with the given claims.
     *
     * @param userId   user UUID (becomes {@code sub})
     * @param username login username
     * @param orgId    selected organisation UUID
     * @param orgSlug  organisation slug
     * @param roles    list of role names
     * @return signed JWT string
     */
    public String createToken(UUID userId, String username, UUID orgId, String orgSlug,
                              List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(
                (long) securityProperties.getJwt().getExpirationMinutes() * 60);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("org_id", orgId != null ? orgId.toString() : null)
                .claim("org_slug", orgSlug)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Parses and validates the given JWT string.
     *
     * @param token JWT string (without "Bearer " prefix)
     * @return parsed {@link Claims}
     * @throws JwtException if the token is invalid, expired, or tampered
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns true if the token string is a valid, non-expired JWT signed with our key.
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── private ────────────────────────────────────────────────────────────────

    private SecretKey signingKey() {
        byte[] keyBytes = securityProperties.getJwt().getSecret()
                .getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
