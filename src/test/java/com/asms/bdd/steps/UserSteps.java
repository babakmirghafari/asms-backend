package com.asms.bdd.steps;

import io.cucumber.java.en.Given;
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
