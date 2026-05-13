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
 * <p>Uses Spring's RestClient — fully compatible with Java 25 (no Groovy/REST Assured dependencies).
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
            .as("Expected HTTP status %d but got %d. "
                + "Note: secured endpoints return 401/403 without valid credentials "
                + "— wire auth in BDD tests to verify %d responses.", expectedStatus, lastStatusCode, expectedStatus)
            .isEqualTo(expectedStatus);
    }

    @Then("the response contains field {string}")
    public void theResponseContainsField(String fieldName) {
        assertThat(lastResponse)
            .as("No response available — endpoint may have returned an error status")
            .isNotNull();
        assertThat(lastResponse.getBody())
            .as("Response body is null — check endpoint and authentication")
            .isNotNull();
        assertThat(lastResponse.getBody().get(fieldName))
            .as("Expected field '%s' in response body", fieldName)
            .isNotNull();
    }

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

    public RestClient getRestClient() {
        return restClient;
    }
}
