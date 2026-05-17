package com.asms.config;

import com.asms.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Test-only security configuration.
 *
 * <p>Replaces the production SecurityConfig (which is excluded via @Profile("!test"))
 * to permit all requests during integration tests.
 *
 * <p>Provides a no-op JwtDecoder bean to prevent Spring Boot auto-configuration
 * from attempting to fetch JWKS from a remote URI during test startup.
 *
 * <p>Registers a TestTenantContextFilter that reads X-Test-Org-Id and X-Test-User-Id
 * request headers to set TenantContext in BDD tests without requiring real JWTs.
 *
 * <p>Import explicitly in test base classes — it is NOT auto-scanned.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * BCrypt password encoder — required in test context because SecurityConfig is excluded
     * via @Profile("!test"). AuthService and SuperAdminBootstrap both inject PasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
     * Test-only filter that sets TenantContext from X-Test-Org-Id / X-Test-User-Id headers.
     * Runs before TenantContextFilter so that services see the test org context.
     */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> testTenantContextFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String orgIdHeader  = request.getHeader("X-Test-Org-Id");
                String userIdHeader = request.getHeader("X-Test-User-Id");
                if (orgIdHeader != null && userIdHeader != null) {
                    try {
                        TenantContext.set(
                            UUID.fromString(orgIdHeader),
                            UUID.fromString(userIdHeader),
                            "bdd-test-user");
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                try {
                    chain.doFilter(request, response);
                } finally {
                    // Only clear if we set it — avoids clearing context set by TenantContextFilter
                    if (orgIdHeader != null) TenantContext.clear();
                }
            }
        });
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
