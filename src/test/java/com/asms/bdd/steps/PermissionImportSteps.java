package com.asms.bdd.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class PermissionImportSteps {

    @Autowired
    private CommonSteps commonSteps;

    @Autowired
    private TestDataHelper testData;

    private UUID lastOrgId;
    private UUID lastActorId;
    private UUID lastImportId;
    private UUID blockedImportId;
    private UUID expiredImportId;
    private UUID committedImportId;

    @When("I upload a valid permissions CSV for my organization")
    public void iUploadAValidPermissionsCSVForMyOrganization() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastActorId = testData.createUser(lastOrgId, "imp-actor-" + unique,
                "imp-actor-" + unique + "@example.com", "ACTIVE", false, false);
        String csv = "name,resource,action,description\nbdd." + unique + ".read,bdd,READ,BDD import test";
        postValidate(lastOrgId, lastActorId, csv.getBytes(StandardCharsets.UTF_8));
        if (commonSteps.getLastResponse() != null
            && commonSteps.getLastResponse().getBody() != null
            && commonSteps.getLastResponse().getBody().get("importId") != null) {
            lastImportId = UUID.fromString(
                commonSteps.getLastResponse().getBody().get("importId").toString());
        }
    }

    @When("I upload a permissions CSV containing invalid rows for my organization")
    public void iUploadAPermissionsCSVContainingInvalidRowsForMyOrganization() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastActorId = testData.createUser(lastOrgId, "imp-err-" + unique,
                "imp-err-" + unique + "@example.com", "ACTIVE", false, false);
        // Missing required 'action' column on second row to trigger ERROR
        String csv = "name,resource,action\n,missing,";
        postValidate(lastOrgId, lastActorId, csv.getBytes(StandardCharsets.UTF_8));
        if (commonSteps.getLastResponse() != null
            && commonSteps.getLastResponse().getBody() != null
            && commonSteps.getLastResponse().getBody().get("importId") != null) {
            lastImportId = UUID.fromString(
                commonSteps.getLastResponse().getBody().get("importId").toString());
        }
    }

    @When("I commit the import using the returned importId")
    public void iCommitTheImportUsingTheReturnedImportId() {
        postCommit(lastImportId, lastOrgId, lastActorId);
    }

    @Given("a permission import session exists with status {string}")
    public void aPermissionImportSessionExistsWithStatus(String status) {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastActorId = testData.createUser(lastOrgId, "imp-blk-" + unique,
                "imp-blk-" + unique + "@example.com", "ACTIVE", false, false);
        if ("BLOCKED".equalsIgnoreCase(status)) {
            blockedImportId = testData.createPermissionImportBlocked(lastOrgId);
        } else {
            blockedImportId = testData.createPermissionImportReady(lastOrgId);
        }
    }

    @When("I commit the import using the BLOCKED importId")
    public void iCommitTheImportUsingTheBlockedImportId() {
        postCommit(blockedImportId, lastOrgId, lastActorId);
    }

    @Given("a permission import session has been committed")
    public void aPermissionImportSessionHasBeenCommitted() {
        lastOrgId = testData.createOrg();
        committedImportId = testData.createPermissionImportCommitted(lastOrgId);
    }

    @When("I request the import report by importId")
    public void iRequestTheImportReportByImportId() {
        RestClient client = commonSteps.getRestClient();
        try {
            ResponseEntity<Map> response = client.get()
                .uri("/asms/v1/permissions/import/" + committedImportId + "/report")
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    @Given("a permission import session exists that has expired")
    public void aPermissionImportSessionExistsThatHasExpired() {
        lastOrgId = testData.createOrg();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        lastActorId = testData.createUser(lastOrgId, "imp-exp-" + unique,
                "imp-exp-" + unique + "@example.com", "ACTIVE", false, false);
        expiredImportId = testData.createPermissionImportExpired(lastOrgId);
    }

    @When("I commit the import using the expired importId")
    public void iCommitTheImportUsingTheExpiredImportId() {
        postCommit(expiredImportId, lastOrgId, lastActorId);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private void postValidate(UUID orgId, UUID actorId, byte[] csvBytes) {
        RestClient client = actorId != null
            ? commonSteps.getRestClientForTenant(orgId, actorId)
            : commonSteps.getRestClient();
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(csvBytes) {
            @Override
            public String getFilename() {
                return "permissions.csv";
            }
        });
        form.add("organizationId", orgId.toString());
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/permissions/import/validate")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .toEntity(Map.class);
            commonSteps.setLastResponse(response);
        } catch (RestClientResponseException ex) {
            commonSteps.setLastStatusCode(ex.getStatusCode().value());
        }
    }

    private void postCommit(UUID importId, UUID orgId, UUID actorId) {
        RestClient client = (orgId != null && actorId != null)
            ? commonSteps.getRestClientForTenant(orgId, actorId)
            : commonSteps.getRestClient();
        String body = "{\"importId\":\"" + importId + "\"}";
        try {
            ResponseEntity<Map> response = client.post()
                .uri("/asms/v1/permissions/import/commit")
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
