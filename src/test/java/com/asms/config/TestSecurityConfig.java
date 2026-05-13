package com.asms.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.Map;

/**
 * Test-only security configuration.
 *
 * <p>Replaces the production SecurityConfig (which is excluded via @Profile("!test"))
 * to permit all requests during integration tests.
 *
 * <p>Provides a no-op JwtDecoder bean to prevent Spring Boot auto-configuration
 * from attempting to fetch JWKS from a remote URI during test startup.
 *
 * <p>Import explicitly in test base classes — it is NOT auto-scanned.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }

    /**
     * No-op JwtDecoder that prevents OAuth2ResourceServerAutoConfiguration from
     * attempting to fetch the JWKS URI during test context startup.
     * The security filter chain permits all requests, so this decoder is never called.
     */
    @Bean
    public JwtDecoder testJwtDecoder() {
        return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "test-user")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }
}
