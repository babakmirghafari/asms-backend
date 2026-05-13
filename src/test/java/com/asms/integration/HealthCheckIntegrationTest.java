package com.asms.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying the application starts and the health endpoint responds.
 *
 * <p>Requires Docker for Testcontainers PostgreSQL.
 * Uses Spring's RestClient — fully compatible with Java 25 (no Groovy dependencies).
 */
@DisplayName("Health Check Integration Test")
class HealthCheckIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int serverPort;

    @Test
    @DisplayName("actuator/health returns UP")
    void healthEndpointReturnsUp() {
        RestClient restClient = RestClient.create("http://localhost:" + serverPort);

        ResponseEntity<Map> response = restClient.get()
            .uri("/actuator/health")
            .retrieve()
            .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
