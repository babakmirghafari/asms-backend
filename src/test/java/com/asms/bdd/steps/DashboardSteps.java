package com.asms.bdd.steps;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

public class DashboardSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastOrgId;

    @When("I request the dashboard summary")
    public void iRequestTheDashboardSummary() {
        if (lastOrgId == null) {
            lastOrgId = testData.createOrg();
        }
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/dashboard/summary?organizationId=" + lastOrgId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }
}
