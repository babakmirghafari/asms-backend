package com.asms.service;

import com.asms.domain.AuthPolicy;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.UpdateAuthPolicyRequestDto;
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
        return authPolicyRepository.findByOrgId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth policy not found for org: " + organizationId));
    }

    @Transactional(readOnly = true)
    public Page<AuthPolicy> listAuthPolicies(Integer page, Integer size) {
        return authPolicyRepository.findAll(
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @Transactional
    public AuthPolicy updateAuthPolicy(UUID organizationId, UpdateAuthPolicyRequestDto req) {
        AuthPolicy policy = authPolicyRepository.findByOrgId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth policy not found for org: " + organizationId));
        AuthPolicy before = cloneForAudit(policy);
        if (req.getMaxFailedLoginAttempts() != null)    policy.setMaxFailedAttempts(req.getMaxFailedLoginAttempts());
        if (req.getMfaRequired() != null)               policy.setRequireMfa(req.getMfaRequired());
        if (req.getPasswordMinLength() != null)         policy.setPasswordMinLength(req.getPasswordMinLength());
        if (req.getPasswordRequiresUppercase() != null) policy.setPasswordRequireUppercase(req.getPasswordRequiresUppercase());
        if (req.getPasswordRequiresNumber() != null)    policy.setPasswordRequireNumbers(req.getPasswordRequiresNumber());
        if (req.getPasswordRequiresSpecial() != null)   policy.setPasswordRequireSymbols(req.getPasswordRequiresSpecial());
        if (req.getSessionTimeoutMinutes() != null)     policy.setLockoutDurationMinutes(req.getSessionTimeoutMinutes());
        policy.setUpdatedAt(OffsetDateTime.now());
        AuthPolicy saved = authPolicyRepository.save(policy);
        auditService.recordInfo("AUTH_POLICY", organizationId, "AUTH_POLICY_UPDATED", before, saved);
        return saved;
    }

    private AuthPolicy cloneForAudit(AuthPolicy p) {
        return AuthPolicy.builder()
                .id(p.getId()).orgId(p.getOrgId())
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
