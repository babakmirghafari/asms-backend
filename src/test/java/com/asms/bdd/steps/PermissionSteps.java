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
 * Step definitions for Permission Management BDD scenarios.
 */
public class PermissionSteps {

    @Autowired
    private CommonSteps commonSteps;

    private UUID lastPermissionId;

    @When("I create a permission with valid details")
    public void iCreateAPermissionWithValidDetails() {
        RestClient restClient = commonSteps.getRestClient();
        String body = "{\"name\":\"READ_DASHBOARD\",\"resource\":\"dashboard\",\"action\":\"read\"}";
        try {
            ResponseEntity<Map> response = restClient.post()
                .uri("/asms/v1/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @Given("a permission exists")
    public void aPermissionExists() {
        // TODO: seed test data when auth is wired
        lastPermissionId = UUID.randomUUID();
    }

    @When("I request the permission by id")
    public void iRequestThePermissionById() {
        RestClient restClient = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = restClient.get()
                .uri("/asms/v1/permissions/" + lastPermissionId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }
}
