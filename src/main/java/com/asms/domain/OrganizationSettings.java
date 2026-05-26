package com.asms.domain;

import com.asms.domain.enums.IdentityProvider;
import com.asms.domain.enums.converter.IdentityProviderConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_settings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrganizationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    // Security
    @Column(name = "require_mfa", nullable = false)
    private boolean requireMfa;

    @Column(name = "force_mfa_on_sensitive", nullable = false)
    private boolean forceMfaOnSensitive;

    @Builder.Default
    @Column(name = "session_timeout", nullable = false)
    private int sessionTimeout = 30;

    @Builder.Default
    @Column(name = "max_concurrent_sessions", nullable = false)
    private int maxConcurrentSessions = 3;

    @Column(name = "allowed_ip_cidrs")
    private String allowedIpCidrs;

    // SSO
    @Column(name = "sso_enabled", nullable = false)
    private boolean ssoEnabled;

    @Column(name = "identity_provider")
    @Convert(converter = IdentityProviderConverter.class)
    private IdentityProvider identityProvider;

    @Column(name = "auto_provision", nullable = false)
    private boolean autoProvision;

    @Column(name = "auto_deprovision", nullable = false)
    private boolean autoDeprovision;

    // Branding
    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "custom_login_url")
    private String customLoginUrl;

    @Column(name = "welcome_message")
    private String welcomeMessage;

    // Compliance
    @Column(name = "data_residency")
    private String dataResidency;

    @Column(name = "enforce_data_residency_on_exports", nullable = false)
    private boolean enforceDataResidencyOnExports;

    @Column(name = "long_term_audit_retention", nullable = false)
    private boolean longTermAuditRetention;

    @Column(name = "gdpr_data_export_endpoint", nullable = false)
    private boolean gdprDataExportEndpoint;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
