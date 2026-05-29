package com.asms.handler;

import com.asms.domain.Membership;
import com.asms.domain.Organization;
import com.asms.domain.enums.MembershipStatus;
import com.asms.repository.MembershipRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pre-authentication endpoint: returns the organizations a user belongs to,
 * identified by the sessionToken (user UUID) issued during the login flow.
 * No JWT required — the sessionToken itself is the identity proof here.
 */
@RestController
@RequestMapping("/asms/v1/auth")
public class AuthOrgsController {

    private final MembershipRepository membershipRepository;

    public AuthOrgsController(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/orgs")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> getOrgsForSession(
            @RequestParam String sessionToken) {

        UUID userId;
        try {
            userId = UUID.fromString(sessionToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        List<Membership> memberships = membershipRepository.findByUserIdWithOrg(userId);

        List<Map<String, Object>> result = memberships.stream()
                .filter(m -> m.getStatus() != MembershipStatus.REMOVED)
                .map(m -> {
                    Organization org = m.getOrganization();
                    return Map.<String, Object>of(
                            "id",          org.getId().toString(),
                            "name",        org.getName() != null ? org.getName() : "",
                            "slug",        org.getSlug() != null ? org.getSlug() : "",
                            "domain",      org.getDomain() != null ? org.getDomain() : "",
                            "memberCount", org.getMemberCount(),
                            "plan",        org.getPlan() != null ? org.getPlan().name() : ""
                    );
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}
