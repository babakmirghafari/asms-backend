package com.asms.bdd.steps;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

public class ActivityLogsSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastOrgId;

    private UUID ensureOrgAndActivity() {
        if (lastOrgId == null) {
            lastOrgId = testData.createOrg();
            String unique = UUID.randomUUID().toString().substring(0, 8);
            UUID userId = testData.createUser(lastOrgId, "act-" + unique, "act-" + unique + "@example.com",
                    "ACTIVE", false, false);
            testData.createAuditActivityLog(lastOrgId, userId, "act-" + unique);
        }
        return lastOrgId;
    }

    @When("I request the activity logs for my organization")
    public void iRequestTheActivityLogsForMyOrganization() {
        UUID orgId = ensureOrgAndActivity();
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/activity-logs?organizationId=" + orgId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I request activity logs with category {string}")
    public void iRequestActivityLogsWithCategory(String category) {
        UUID orgId = ensureOrgAndActivity();
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/activity-logs?organizationId=" + orgId + "&category=" + category)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I request activity logs from {string} to {string}")
    public void iRequestActivityLogsFromTo(String from, String to) {
        UUID orgId = ensureOrgAndActivity();
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/activity-logs?organizationId=" + orgId
                     + "&fromDate=" + from + "&toDate=" + to)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }
}
