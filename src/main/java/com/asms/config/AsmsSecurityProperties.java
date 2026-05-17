package com.asms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for ASMS security properties.
 *
 * <p>All values are read from {@code application.yml} under the {@code asms.security} prefix.
 * Secrets are injected via environment variables — never hardcoded.
 */
@Component
@ConfigurationProperties(prefix = "asms.security")
public class AsmsSecurityProperties {

    private final Jwt jwt = new Jwt();
    private int maxFailedAttempts = 5;
    private int lockoutDurationMinutes = 30;

    public Jwt getJwt() {
        return jwt;
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public static class Jwt {

        private String secret = "change-me-in-production-min-32-chars-long";
        private int expirationMinutes = 60;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpirationMinutes() {
            return expirationMinutes;
        }

        public void setExpirationMinutes(int expirationMinutes) {
            this.expirationMinutes = expirationMinutes;
        }
    }
}
