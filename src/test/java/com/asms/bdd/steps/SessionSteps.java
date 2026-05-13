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
 * Step definitions for Session Management BDD scenarios.
 */
public class SessionSteps {

    @Autowired
    private CommonSteps commonSteps;

    private UUID lastSessionId;
    private UUID organizationId;

    @Given("a session exists")
    public void aSessionExists() {
        // TODO: seed test data when auth is wired
        lastSessionId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
    }

    @When("I revoke the session by id")
    public void iRevokeTheSessionById() {
        RestClient restClient = commonSteps.getRestClient();
        String body = "{\"reason\":\"MANUAL_REVOCATION\"}";
        try {
            ResponseEntity<Map> response = restClient.post()
                .uri("/asms/v1/sessions/" + lastSessionId + "/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I list sessions for my organization")
    public void iListSessionsForMyOrganization() {
        RestClient restClient = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = restClient.get()
                .uri("/asms/v1/sessions?organizationId=" + UUID.randomUUID())
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }
}
