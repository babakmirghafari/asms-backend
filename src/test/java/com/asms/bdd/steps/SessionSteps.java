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

public class SessionSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastSessionId;
    private UUID lastOrgId;
    private UUID lastUserId;

    @Given("a session exists")
    public void aSessionExists() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastUserId = testData.createUser(lastOrgId, "sess-" + unique, "sess-" + unique + "@example.com",
                "ACTIVE", false, false);
        lastSessionId = testData.createSession(lastUserId, lastOrgId, "ACTIVE");
    }

    @When("I revoke the session by id")
    public void iRevokeTheSessionById() {
        RestClient client = commonSteps.getRestClientForTenant(lastOrgId, lastUserId);
        String body = "{\"reason\":\"MANUAL_REVOCATION\"}";
        try {
            ResponseEntity<Map> response = client.post()
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
        if (lastOrgId == null) {
            lastOrgId = testData.createOrg();
        }
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/sessions?organizationId=" + lastOrgId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }
}
