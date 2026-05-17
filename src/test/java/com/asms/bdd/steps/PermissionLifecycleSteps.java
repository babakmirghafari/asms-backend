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

public class PermissionLifecycleSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastPermissionId;
    private UUID lastOrgId;
    private UUID lastActorId;

    @Given("a permission exists with status {string}")
    public void aPermissionExistsWithStatus(String status) {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastActorId = testData.createUser(lastOrgId, "lc-actor-" + unique,
                "lc-actor-" + unique + "@example.com", "ACTIVE", false, false);
        lastPermissionId = testData.createPermission(
            lastOrgId, "lc." + unique + ".read", "lc", "READ", status);
    }

    @When("I update the permission status to {string} with reason {string}")
    public void iUpdateThePermissionStatusToWithReason(String targetStatus, String reason) {
        RestClient client = commonSteps.getRestClientForTenant(lastOrgId, lastActorId);
        String body = """
                {"status":"%s","reason":"%s"}
                """.formatted(targetStatus, reason);
        try {
            ResponseEntity<Map> response = client.patch()
                .uri("/asms/v1/permissions/" + lastPermissionId)
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
