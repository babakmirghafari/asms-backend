package com.asms.config;

import com.asms.security.TenantContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <p>All endpoints require a JWT Bearer token except the public auth endpoints.
 * Stateless session management — no server-side HTTP session.
 *
 * <p>TenantContextFilter runs after JWT validation to populate TenantContext
 * with org_id and user UUID from JWT claims (ADR-006, AC-13).
 *
 * <p>Excluded from the "test" profile — TestSecurityConfig is used instead,
 * permitting all requests so tests can verify HTTP responses without JWT credentials.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
@RequiredArgsConstructor
public class SecurityConfig {

    private final TenantContextFilter tenantContextFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers(
                    "/asms/v1/auth/login",
                    "/asms/v1/auth/mfa/verify",
                    "/asms/v1/auth/select-org",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JwtDecoder auto-configured from spring.security.oauth2.resourceserver.jwt.jwk-set-uri
                })
            )
            .addFilterAfter(tenantContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
