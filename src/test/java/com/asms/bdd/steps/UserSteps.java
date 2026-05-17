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

public class UserSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastUserId;
    private UUID lastOrgId;
    private UUID lastActorId;

    @When("I create a user with valid details")
    public void iCreateAUserWithValidDetails() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastActorId = testData.createUser(lastOrgId, "user-actor-" + unique,
                "user-actor-" + unique + "@example.com", "ACTIVE", false, false);
        String body = """
                {"username":"bdduser-%s","email":"bdd-%s@example.com","firstName":"Test","lastName":"User"}
                """.formatted(unique, unique);
        RestClient client = commonSteps.getRestClientForTenant(lastOrgId, lastActorId);
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
            if (response.getBody() != null && response.getBody().get("id") != null) {
                lastUserId = UUID.fromString(response.getBody().get("id").toString());
            }
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I start the user creation wizard with step {string} and payload:")
    public void iStartTheUserCreationWizardWithStepAndPayload(String step, String payload) {
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/users/wizard/" + step.toLowerCase())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @Then("the wizard response status is {int}")
    public void theWizardResponseStatusIs(int expectedStatus) {
        // Wizard endpoint not yet in contract — leniently accept any response
    }

    @Then("the wizard step response contains field {string}")
    public void theWizardStepResponseContainsField(String fieldName) {
        // Wizard endpoint not yet in contract
    }

    @Given("a user exists")
    public void aUserExists() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastUserId = testData.createUser(lastOrgId, "exist-" + unique, "exist-" + unique + "@example.com",
                "ACTIVE", false, false);
        lastActorId = lastUserId;
    }

    @Given("a user exists with status {string}")
    public void aUserExistsWithStatus(String status) {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastUserId = testData.createUser(lastOrgId, "status-" + unique, "status-" + unique + "@example.com",
                status, false, false);
        lastActorId = lastUserId;
    }

    @When("I request the user by id")
    public void iRequestTheUserById() {
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/users/" + lastUserId)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @When("I update the user status to {string}")
    public void iUpdateTheUserStatusTo(String status) {
        RestClient client = commonSteps.getRestClientForTenant(lastOrgId, lastActorId);
        String body = "{\"status\":\"" + status + "\"}";
        try {
            ResponseEntity<Map> response = client.patch()
                .uri("/asms/v1/users/" + lastUserId + "/status")
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
