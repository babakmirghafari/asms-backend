package com.asms.config;

import com.asms.security.TenantContextFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <p>All endpoints require a JWT Bearer token except the public auth endpoints.
 * Stateless session management — no server-side HTTP session.
 *
 * <p>TenantContextFilter validates the JWT, populates SecurityContext AND TenantContext
 * from the token claims (change 3/5: no Spring OAuth2 resource server — we sign and verify
 * JWTs with JJWT directly).
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
                // Public auth endpoints — excluded from JWT validation
                .requestMatchers(
                    "/asms/v1/auth/login",
                    "/asms/v1/auth/mfa/verify",
                    "/asms/v1/auth/select-org",
                    "/asms/v1/auth/change-password",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Return 401 for unauthenticated requests — Spring Security 6.1+ defaults to
            // Http403ForbiddenEntryPoint when no entry point is configured, so we must set one.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.getWriter().write(
                        "{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                })
            )
            // Use our own TenantContextFilter (JJWT-based) — no Spring OAuth2 resource server
            .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder — used in AuthService for hash/verify and SuperAdmin bootstrap.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
