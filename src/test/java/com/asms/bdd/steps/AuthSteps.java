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

public class AuthSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    /** username → UUID, set by Given steps, read by And steps */
    private UUID lastCreatedUserId;
    private String lastUsername;

    @When("I login with username {string} and password {string}")
    public void iLoginWithUsernameAndPassword(String username, String password) {
        RestClient client = commonSteps.getRestClient();
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @Then("the login response status is {int}")
    public void theLoginResponseStatusIs(int status) {
        assertThat(commonSteps.getLastStatusCode())
            .as("Expected login HTTP status %d but got %d", status, commonSteps.getLastStatusCode())
            .isEqualTo(status);
    }

    @Then("the response requires MFA verification")
    public void theResponseRequiresMfaVerification() {
        assertThat(commonSteps.getLastResponse()).isNotNull();
        assertThat(commonSteps.getLastResponse().getBody()).isNotNull();
        Object statusField = commonSteps.getLastResponse().getBody().get("status");
        assertThat(statusField)
            .as("Expected status=MFA_REQUIRED in login response")
            .isNotNull();
        assertThat(statusField.toString()).isEqualTo("MFA_REQUIRED");
    }

    @Then("the response requires organization selection")
    public void theResponseRequiresOrganizationSelection() {
        assertThat(commonSteps.getLastResponse()).isNotNull();
        assertThat(commonSteps.getLastResponse().getBody()).isNotNull();
        Object statusField = commonSteps.getLastResponse().getBody().get("status");
        assertThat(statusField).isNotNull();
        assertThat(statusField.toString()).isEqualTo("ORG_SELECTION_REQUIRED");
    }

    @Given("a user with username {string} exists with status {string}")
    public void aUserWithUsernameExistsWithStatus(String username, String status) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        // Upsert so this step is idempotent if the scenario is discovered and run twice
        lastCreatedUserId = testData.upsertUser(
            username, unique + "@bdd.example.com", status, true, false);
        lastUsername = username;
    }

    @Given("the user was issued a temporary password {string}")
    public void theUserWasIssuedATemporaryPassword(String tempPassword) {
        if (lastCreatedUserId != null) {
            testData.setUserForcePasswordChange(lastCreatedUserId);
        }
    }

    @Then("the response requires password change")
    public void theResponseRequiresPasswordChange() {
        assertThat(commonSteps.getLastResponse()).isNotNull();
        assertThat(commonSteps.getLastResponse().getBody()).isNotNull();
        Object statusField = commonSteps.getLastResponse().getBody().get("status");
        assertThat(statusField)
            .as("Expected status=TEMP_PASSWORD_REQUIRED in login response")
            .isNotNull();
        assertThat(statusField.toString()).isEqualTo("TEMP_PASSWORD_REQUIRED");
    }

    @Given("the user has failed login {int} times")
    public void theUserHasFailedLoginTimes(int failedAttempts) {
        if (lastCreatedUserId != null) {
            testData.setUserFailedLoginsAndLock(lastCreatedUserId, failedAttempts);
        }
    }

    @Then("the account is locked")
    public void theAccountIsLocked() {
        assertThat(commonSteps.getLastStatusCode())
            .as("Expected HTTP 423 (account locked) but got %d", commonSteps.getLastStatusCode())
            .isEqualTo(423);
    }
}
