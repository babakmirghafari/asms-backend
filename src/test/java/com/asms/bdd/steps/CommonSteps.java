package com.asms.bdd.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Common Cucumber step definitions shared across all feature files.
 *
 * <p>Uses Spring's RestClient — fully compatible with Java 25 (no Groovy/REST Assured).
 */
public class CommonSteps {

    @Value("${local.server.port:8080}")
    private int serverPort;

    private RestClient restClient;
    private ResponseEntity<Map> lastResponse;
    private int lastStatusCode = -1;

    @Before
    public void setUp() {
        restClient = RestClient.create("http://localhost:" + serverPort);
        lastResponse = null;
        lastStatusCode = -1;
    }

    @Given("the service is running")
    public void theServiceIsRunning() {
        ResponseEntity<Map> response = restClient.get()
            .uri("/actuator/health")
            .retrieve()
            .toEntity(Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @When("I request the health endpoint")
    public void iRequestTheHealthEndpoint() {
        lastResponse = restClient.get()
            .uri("/actuator/health")
            .retrieve()
            .toEntity(Map.class);
        lastStatusCode = lastResponse.getStatusCode().value();
    }

    @Then("the health status is UP")
    public void theHealthStatusIsUp() {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getBody()).isNotNull();
        assertThat(lastResponse.getBody().get("status")).isEqualTo("UP");
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertThat(lastStatusCode)
            .as("Expected HTTP %d but got %d", expectedStatus, lastStatusCode)
            .isEqualTo(expectedStatus);
    }

    @Then("the response contains field {string}")
    public void theResponseContainsField(String fieldName) {
        assertThat(lastResponse)
            .as("No response body available")
            .isNotNull();
        assertThat(lastResponse.getBody())
            .as("Response body is null")
            .isNotNull();
        assertThat(lastResponse.getBody())
            .as("Expected field '%s' in response", fieldName)
            .containsKey(fieldName);
    }

    @Then("the field {string} equals {string}")
    public void theFieldEquals(String fieldName, String expectedValue) {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getBody()).isNotNull();
        Object actual = lastResponse.getBody().get(fieldName);
        assertThat(actual)
            .as("Field '%s': expected '%s' but got '%s'", fieldName, expectedValue, actual)
            .isNotNull();
        assertThat(actual.toString())
            .as("Field '%s'", fieldName)
            .isEqualToIgnoringCase(expectedValue);
    }

    @Then("the field {string} contains {string}")
    @SuppressWarnings("unchecked")
    public void theFieldContains(String fieldName, String key) {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getBody()).isNotNull();
        Object value = lastResponse.getBody().get(fieldName);
        assertThat(value)
            .as("Field '%s' is null or missing", fieldName)
            .isNotNull();
        assertThat(value).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) value).containsKey(key))
            .as("Expected key '%s' in field '%s'", key, fieldName)
            .isTrue();
    }

    // ─── package-private accessors for step classes ─────────────────────────

    public void setLastResponse(ResponseEntity<Map> response) {
        this.lastResponse = response;
        if (response != null) {
            this.lastStatusCode = response.getStatusCode().value();
        }
    }

    public void setLastStatusCode(int statusCode) {
        this.lastStatusCode = statusCode;
    }

    public ResponseEntity<Map> getLastResponse() {
        return lastResponse;
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public RestClient getRestClientForTenant(java.util.UUID orgId, java.util.UUID userId) {
        return RestClient.builder()
            .baseUrl("http://localhost:" + serverPort)
            .defaultHeader("X-Test-Org-Id", orgId.toString())
            .defaultHeader("X-Test-User-Id", userId.toString())
            .build();
    }

    public int getServerPort() {
        return serverPort;
    }
}
