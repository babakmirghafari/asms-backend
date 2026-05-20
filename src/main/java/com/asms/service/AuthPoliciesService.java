package com.asms.service;

import com.asms.domain.AuthPolicy;
import com.asms.exception.ResourceNotFoundException;
import com.asms.repository.AuthPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Authentication policy management service (AC-3, AC-12).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthPoliciesService {

    private final AuthPolicyRepository authPolicyRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AuthPolicy getAuthPolicyByOrganization(UUID organizationId) {
        return authPolicyRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth policy not found for org: " + organizationId));
    }

    @Transactional(readOnly = true)
    public Page<AuthPolicy> listAuthPolicies(Integer page, Integer size) {
        return authPolicyRepository.findAll(
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    /**
     * Patch record carrying the optional fields extracted from {@code UpdateAuthPolicyRequestDto}.
     * Null means "no change"; non-null means "apply this value."
     */
    public record AuthPolicyPatch(
            Integer maxFailedLoginAttempts,
            Boolean mfaRequired,
            Integer passwordMinLength,
            Boolean passwordRequiresUppercase,
            Boolean passwordRequiresNumber,
            Boolean passwordRequiresSpecial,
            Integer sessionTimeoutMinutes) {}

    /**
     * Applies an update patch to the auth policy for an organization.
     * The handler extracts the patch fields from the request DTO before calling here.
     *
     * @param organizationId the organization whose auth policy is being updated
     * @param patch          field patch — null fields are not applied
     */
    @Transactional
    public AuthPolicy updateAuthPolicy(UUID organizationId, AuthPolicyPatch patch) {
        AuthPolicy policy = authPolicyRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth policy not found for org: " + organizationId));
        AuthPolicy before = cloneForAudit(policy);
        if (patch.maxFailedLoginAttempts() != null)    policy.setMaxFailedAttempts(patch.maxFailedLoginAttempts());
        if (patch.mfaRequired() != null)               policy.setRequireMfa(patch.mfaRequired());
        if (patch.passwordMinLength() != null)         policy.setPasswordMinLength(patch.passwordMinLength());
        if (patch.passwordRequiresUppercase() != null) policy.setPasswordRequireUppercase(patch.passwordRequiresUppercase());
        if (patch.passwordRequiresNumber() != null)    policy.setPasswordRequireNumbers(patch.passwordRequiresNumber());
        if (patch.passwordRequiresSpecial() != null)   policy.setPasswordRequireSymbols(patch.passwordRequiresSpecial());
        if (patch.sessionTimeoutMinutes() != null)     policy.setLockoutDurationMinutes(patch.sessionTimeoutMinutes());
        policy.setUpdatedAt(OffsetDateTime.now());
        AuthPolicy saved = authPolicyRepository.save(policy);
        auditService.recordInfo("AUTH_POLICY", organizationId, "AUTH_POLICY_UPDATED", before, saved);
        return saved;
    }

    private AuthPolicy cloneForAudit(AuthPolicy p) {
        return AuthPolicy.builder()
                .id(p.getId()).organization(p.getOrganization())
                .maxFailedAttempts(p.getMaxFailedAttempts()).lockoutDurationMinutes(p.getLockoutDurationMinutes())
                .requireMfa(p.isRequireMfa()).passwordMinLength(p.getPasswordMinLength())
                .passwordRequireUppercase(p.isPasswordRequireUppercase())
                .passwordRequireNumbers(p.isPasswordRequireNumbers())
                .passwordRequireSymbols(p.isPasswordRequireSymbols())
                .passwordHistoryCount(p.getPasswordHistoryCount())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
