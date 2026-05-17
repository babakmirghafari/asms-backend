package com.asms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for ASMS MFA properties.
 *
 * <p>All values are read from {@code application.yml} under the {@code asms.mfa} prefix.
 */
@Component
@ConfigurationProperties(prefix = "asms.mfa")
public class AsmsMfaProperties {

    private String redirectUrl = "http://localhost:4200/mfa-verify";

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
