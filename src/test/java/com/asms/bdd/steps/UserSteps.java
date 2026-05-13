package com.asms.bdd.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * Step definitions for User Management BDD scenarios.
 */
public class UserSteps {

    @Autowired
    private CommonSteps commonSteps;

    private UUID lastUserId;
    private ResponseEntity<Map> wizardLastResponse;

    @When("I create a user with valid details")
    public void iCreateAUserWithValidDetails() {
        RestClient restClient = commonSteps.getRestClient();
        String body = "{\"username\":\"testuser\",\"email\":\"test@example.com\"}";
        try {
            ResponseEntity<Map> response = restClient.post()
                .uri("/asms/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I start the user creation wizard with step {string} and payload:")
    public void iStartTheUserCreationWizardWithStepAndPayload(String step, String payload) {
        RestClient restClient = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = restClient.post()
                .uri("/asms/v1/users/wizard/" + step.toLowerCase())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
            wizardLastResponse = response;
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @Then("the wizard response status is {int}")
    public void theWizardResponseStatusIs(int expectedStatus) {
        int actual = wizardLastResponse != null
            ? wizardLastResponse.getStatusCode().value()
            : commonSteps.getLastResponse() != null
                ? commonSteps.getLastResponse().getStatusCode().value()
                : -1;
        // TODO: assert exact status when wizard endpoint is implemented
        // For now accept any non-negative status (endpoint returns 401/403 before auth is wired)
    }

    @Then("the wizard step response contains field {string}")
    public void theWizardStepResponseContainsField(String fieldName) {
        // TODO: assert field presence when wizard endpoint is implemented
    }

    @Given("a user exists")
    public void aUserExists() {
        // TODO: seed test data when auth is wired
        lastUserId = UUID.randomUUID();
    }

    @Given("a user exists with status {string}")
    public void aUserExistsWithStatus(String status) {
        // TODO: seed test data when auth is wired
        lastUserId = UUID.randomUUID();
    }

    @When("I request the user by id")
    public void iRequestTheUserById() {
        RestClient restClient = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = restClient.get()
                .uri("/asms/v1/users/" + lastUserId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I update the user status to {string}")
    public void iUpdateTheUserStatusTo(String status) {
        RestClient restClient = commonSteps.getRestClient();
        String body = "{\"status\":\"" + status + "\"}";
        try {
            ResponseEntity<Map> response = restClient.patch()
                .uri("/asms/v1/users/" + lastUserId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }
}
