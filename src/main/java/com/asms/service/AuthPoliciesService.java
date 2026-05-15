package com.asms.service;

import com.asms.domain.AuthPolicy;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AuthPolicyDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateAuthPolicyRequestDto;
import com.asms.repository.AuthPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
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
    public ResponseEntity<AuthPolicyDto> getAuthPolicyByOrganization(UUID organizationId) {
        AuthPolicy policy = authPolicyRepository.findByOrgId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth policy not found for org: " + organizationId));
        return ResponseEntity.ok(toDto(policy));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listAuthPolicies(Integer page, Integer size) {
        Page<AuthPolicy> results = authPolicyRepository.findAll(
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    @Transactional
    public ResponseEntity<AuthPolicyDto> updateAuthPolicy(
            UUID organizationId, UpdateAuthPolicyRequestDto req) {
        AuthPolicy policy = authPolicyRepository.findByOrgId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Auth policy not found for org: " + organizationId));
        AuthPolicyDto before = toDto(policy);
        // UpdateAuthPolicyRequestDto fields map to domain fields
        if (req.getMaxFailedLoginAttempts() != null)  policy.setMaxFailedAttempts(req.getMaxFailedLoginAttempts());
        if (req.getMfaRequired() != null)             policy.setRequireMfa(req.getMfaRequired());
        if (req.getPasswordMinLength() != null)       policy.setPasswordMinLength(req.getPasswordMinLength());
        if (req.getPasswordRequiresUppercase() != null) policy.setPasswordRequireUppercase(req.getPasswordRequiresUppercase());
        if (req.getPasswordRequiresNumber() != null)    policy.setPasswordRequireNumbers(req.getPasswordRequiresNumber());
        if (req.getPasswordRequiresSpecial() != null)   policy.setPasswordRequireSymbols(req.getPasswordRequiresSpecial());
        if (req.getSessionTimeoutMinutes() != null)   policy.setLockoutDurationMinutes(req.getSessionTimeoutMinutes());
        policy.setUpdatedAt(OffsetDateTime.now());
        AuthPolicy saved = authPolicyRepository.save(policy);
        auditService.recordInfo("AUTH_POLICY", organizationId, "AUTH_POLICY_UPDATED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
    }

    private AuthPolicyDto toDto(AuthPolicy p) {
        AuthPolicyDto dto = new AuthPolicyDto();
        dto.setId(p.getId());
        dto.setOrganizationId(p.getOrgId());
        dto.setMaxFailedLoginAttempts(p.getMaxFailedAttempts());
        dto.setMfaRequired(p.isRequireMfa());
        dto.setPasswordMinLength(p.getPasswordMinLength());
        dto.setPasswordRequiresUppercase(p.isPasswordRequireUppercase());
        dto.setPasswordRequiresNumber(p.isPasswordRequireNumbers());
        dto.setPasswordRequiresSpecial(p.isPasswordRequireSymbols());
        dto.setSessionTimeoutMinutes(p.getLockoutDurationMinutes());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    private PagedResponseDto buildPage(List<?> items, long total, int page, int size) {
        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(items.stream().map(i -> (Object) i).toList());
        dto.setTotalElements(total);
        dto.setNumber(page);
        dto.setSize(size);
        dto.setTotalPages(size > 0 ? (int) Math.ceil((double) total / size) : 0);
        return dto;
    }
}
