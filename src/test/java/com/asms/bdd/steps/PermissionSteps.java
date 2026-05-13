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

    @When("I upload a CSV file for permission import with content:")
    public void iUploadACsvFileForPermissionImportWithContent(String csvContent) {
        RestClient restClient = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = restClient.post()
                .uri("/asms/v1/permissions/import")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @Then("the import response status is {int}")
    public void theImportResponseStatusIs(int expectedStatus) {
        // TODO: assert exact status when CSV import endpoint is implemented
    }

    @Then("the import response contains field {string}")
    public void theImportResponseContainsField(String fieldName) {
        // TODO: assert field presence when CSV import endpoint is implemented
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
