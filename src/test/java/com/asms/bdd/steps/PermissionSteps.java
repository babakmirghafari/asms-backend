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

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastPermissionId;
    private UUID lastOrgId;
    private UUID lastUserId;

    @When("I create a permission with valid details")
    public void iCreateAPermissionWithValidDetails() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastUserId = testData.createUser(lastOrgId, "perm-actor-" + unique,
                "perm-actor-" + unique + "@example.com", "ACTIVE", false, false);
        String body = """
                {"name":"bdd.%s.read","resource":"bdd","action":"READ","organizationId":"%s"}
                """.formatted(unique, lastOrgId);
        RestClient client = commonSteps.getRestClientForTenant(lastOrgId, lastUserId);
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
            if (response.getBody() != null && response.getBody().get("id") != null) {
                lastPermissionId = UUID.fromString(response.getBody().get("id").toString());
            }
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I upload a CSV file for permission import with content:")
    public void iUploadACsvFileForPermissionImportWithContent(String csvContent) {
        // This step is superseded by PermissionImportSteps for the two-step flow
        RestClient client = commonSteps.getRestClient();
        UUID orgId = lastOrgId != null ? lastOrgId : testData.createOrg();
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/permissions/import/validate?organizationId=" + orgId)
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
        assertThat(commonSteps.getLastStatusCode())
            .as("Expected import HTTP status %d but got %d", expectedStatus, commonSteps.getLastStatusCode())
            .isEqualTo(expectedStatus);
    }

    @Then("the import response contains field {string}")
    public void theImportResponseContainsField(String fieldName) {
        assertThat(commonSteps.getLastResponse()).isNotNull();
        assertThat(commonSteps.getLastResponse().getBody()).isNotNull();
        assertThat(commonSteps.getLastResponse().getBody()).containsKey(fieldName);
    }

    @Given("a permission exists")
    public void aPermissionExists() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastPermissionId = testData.createPermission(lastOrgId, "bdd." + unique + ".read", "bdd", "READ", "ACTIVE");
    }

    @When("I request the permission by id")
    public void iRequestThePermissionById() {
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/permissions/" + lastPermissionId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I simulate permission {string} for a user in my organization")
    public void iSimulatePermissionForUserInMyOrganization(String permissionName) {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastUserId = testData.createUser(lastOrgId, "sim-" + unique, "sim-" + unique + "@example.com",
                "ACTIVE", false, false);
        // Create permission and assign to user via group
        UUID permId = testData.createPermission(lastOrgId, permissionName, permissionName.split("\\.")[0], "READ", "ACTIVE");
        UUID groupId = testData.createPermissionGroup(lastOrgId, "sim-group-" + unique);
        testData.assignPermissionToGroup(groupId, permId);
        testData.assignUserToGroup(groupId, lastUserId);

        String body = """
                {"permissionName":"%s","userId":"%s","organizationId":"%s"}
                """.formatted(permissionName, lastUserId, lastOrgId);
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/permissions/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I simulate permission {string} for a user without that permission")
    public void iSimulatePermissionForUserWithoutThatPermission(String permissionName) {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastUserId = testData.createUser(lastOrgId, "nosim-" + unique, "nosim-" + unique + "@example.com",
                "ACTIVE", false, false);
        // User exists but has NO permission assigned

        String body = """
                {"permissionName":"%s","userId":"%s","organizationId":"%s"}
                """.formatted(permissionName, lastUserId, lastOrgId);
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/permissions/simulate")
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
