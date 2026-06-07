package com.asms.web;

import com.asms.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Pre-authentication endpoint: returns the organisations a user belongs to,
 * identified by the sessionToken (user UUID) issued during the login flow.
 * No JWT required — sessionToken is the identity proof at this step.
 *
 * Placed in com.asms.web (not com.asms.handler) because this endpoint is not
 * yet part of the generated contract delegate — it predates that contract entry.
 * Once GET /auth/orgs is added to the OpenAPI spec, move the logic to AuthHandler.
 */
@RestController
@RequestMapping("/asms/v1/auth")
@RequiredArgsConstructor
public class AuthOrgsController {

    private final AuthService authService;

    @GetMapping("/orgs")
    public ResponseEntity<List<AuthService.OrgEntry>> getOrgsForSession(
            @RequestParam String sessionToken) {
        UUID userId;
        try {
            userId = UUID.fromString(sessionToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.getOrgsForUser(userId));
    }
}
