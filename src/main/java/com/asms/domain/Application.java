package com.asms.domain;

import com.asms.domain.enums.ApplicationStatus;
import com.asms.domain.enums.ConnectorType;
import com.asms.domain.enums.IntegrationHealthStatus;
import com.asms.domain.enums.converter.ApplicationStatusConverter;
import com.asms.domain.enums.converter.ConnectorTypeConverter;
import com.asms.domain.enums.converter.IntegrationHealthStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    @Convert(converter = ConnectorTypeConverter.class)
    private ConnectorType type;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret_hash")
    private String clientSecretHash;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "redirect_uris", columnDefinition = "text[]")
    private List<String> redirectUris;

    @Column(name = "saml_entity_id")
    private String samlEntityId;

    @Column(name = "status", nullable = false)
    @Convert(converter = ApplicationStatusConverter.class)
    private ApplicationStatus status;

    @Column(name = "integration_health_status", nullable = false)
    @Convert(converter = IntegrationHealthStatusConverter.class)
    private IntegrationHealthStatus integrationHealthStatus;

    @Column(name = "secret_expires_at")
    private OffsetDateTime secretExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
