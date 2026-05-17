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

public class AlertSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastAlertId;
    private UUID lastOrgId;
    private UUID lastUserId;

    @Given("a security alert exists with status {string}")
    public void aSecurityAlertExistsWithStatus(String status) {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastUserId = testData.createUser(lastOrgId, "alt-" + unique, "alt-" + unique + "@example.com",
                "ACTIVE", false, false);
        lastAlertId = testData.createAlert(lastOrgId, lastUserId, "IMPOSSIBLE_TRAVEL", "HIGH", status);
    }

    @When("I resolve the alert with note {string}")
    public void iResolveTheAlertWithNote(String note) {
        RestClient client = commonSteps.getRestClientForTenant(lastOrgId, lastUserId);
        String body = "{\"note\":\"" + note + "\"}";
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/alerts/" + lastAlertId + "/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I escalate the alert with reason {string} and target {string}")
    public void iEscalateTheAlertWithReasonAndTarget(String reason, String target) {
        RestClient client = commonSteps.getRestClientForTenant(lastOrgId, lastUserId);
        String body = "{\"reason\":\"" + reason + "\",\"escalateTo\":\"" + target + "\"}";
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/alerts/" + lastAlertId + "/escalate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I list alerts for my organization")
    public void iListAlertsForMyOrganization() {
        if (lastOrgId == null) {
            lastOrgId = testData.createOrg();
        }
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/alerts?organizationId=" + lastOrgId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }
}
