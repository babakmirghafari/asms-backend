package com.asms.bdd.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionExportSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID exportOrgId;
    private UUID exportActorId;
    private String lastCsvBody;
    private int lastStatusCode;
    private String lastContentType;

    @Given("I have a new organization with no permissions")
    public void iHaveANewOrganizationWithNoPermissions() {
        exportOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        exportActorId = testData.createUser(exportOrgId, "exp-actor-" + unique,
                "exp-actor-" + unique + "@example.com", "ACTIVE", false, false);
    }

    @When("I request the permissions CSV export for that organization")
    public void iRequestThePermissionsCSVExportForThatOrganization() {
        getExport(exportOrgId, exportActorId, null);
    }

    @Given("I have an organization with an ACTIVE permission {string}")
    public void iHaveAnOrganizationWithAnActivePermission(String name) {
        exportOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        exportActorId = testData.createUser(exportOrgId, "exp-act-" + unique,
                "exp-act-" + unique + "@example.com", "ACTIVE", false, false);
        UUID permId = testData.createPermission(exportOrgId, name, name.split("\\.")[0], "READ", "DRAFT");
        testData.activatePermission(permId);
    }

    @Given("I have an organization with a DRAFT permission {string}")
    public void iHaveAnOrganizationWithADraftPermission(String name) {
        exportOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        exportActorId = testData.createUser(exportOrgId, "exp-drft-" + unique,
                "exp-drft-" + unique + "@example.com", "ACTIVE", false, false);
        testData.createPermission(exportOrgId, name, name.split("\\.")[0], "WRITE", "DRAFT");
    }

    @And("the same organization has an ACTIVE permission {string}")
    public void theSameOrganizationHasAnActivePermission(String name) {
        UUID permId = testData.createPermission(exportOrgId, name, name.split("\\.")[0], "READ", "DRAFT");
        testData.activatePermission(permId);
    }

    @When("I request the permissions CSV export filtered by status {string}")
    public void iRequestThePermissionsCSVExportFilteredByStatus(String status) {
        getExport(exportOrgId, exportActorId, status);
    }

    @And("the response content-type is {string}")
    public void theResponseContentTypeIs(String expected) {
        assertThat(lastContentType).contains(expected);
    }

    @Then("the response body contains {string}")
    public void theResponseBodyContains(String text) {
        assertThat(lastCsvBody).contains(text);
    }

    @Then("the response body does not contain {string}")
    public void theResponseBodyDoesNotContain(String text) {
        assertThat(lastCsvBody).doesNotContain(text);
    }

    private void getExport(UUID orgId, UUID actorId, String status) {
        RestClient client = commonSteps.getRestClientForTenant(orgId, actorId);
        String uri = "/asms/v1/permissions/export?organizationId=" + orgId
                + (status != null ? "&status=" + status : "");
        try {
            ResponseEntity<String> response = client.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(String.class);
            lastStatusCode = response.getStatusCode().value();
            lastCsvBody = response.getBody();
            lastContentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString() : "";
            commonSteps.setLastStatusCode(lastStatusCode);
        } catch (RestClientResponseException ex) {
            lastStatusCode = ex.getStatusCode().value();
            commonSteps.setLastStatusCode(lastStatusCode);
        }
    }
}
