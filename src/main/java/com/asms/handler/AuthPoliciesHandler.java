package com.asms.handler;

import com.asms.api.AuthPoliciesApiDelegate;
import com.asms.domain.AuthPolicy;
import com.asms.mapper.AuthPolicyMapper;
import com.asms.model.AuthPolicyDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateAuthPolicyRequestDto;
import com.asms.service.AuthPoliciesService;
import com.asms.util.PageResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the AuthPolicies API.
 *
 * <p>Implements {@link AuthPoliciesApiDelegate}. Delegates all business logic to
 * {@link AuthPoliciesService}. Maps domain {@link AuthPolicy} ↔ {@link AuthPolicyDto}
 * via {@link AuthPolicyMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPoliciesHandler implements AuthPoliciesApiDelegate {

    private final AuthPoliciesService authPoliciesService;
    private final AuthPolicyMapper authPolicyMapper;

    @Override
    public ResponseEntity<AuthPolicyDto> getAuthPolicyByOrganization(UUID organizationId) {
        return ResponseEntity.ok(authPolicyMapper.toDto(authPoliciesService.getAuthPolicyByOrganization(organizationId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listAuthPolicies(Integer page, Integer size) {
        Page<AuthPolicy> policies = authPoliciesService.listAuthPolicies(page, size);
        List<AuthPolicyDto> dtos = policies.getContent().stream().map(authPolicyMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, policies));
    }

    @Override
    public ResponseEntity<AuthPolicyDto> updateAuthPolicy(
            UUID organizationId, UpdateAuthPolicyRequestDto updateAuthPolicyRequestDto) {
        AuthPolicy policy = authPoliciesService.updateAuthPolicy(organizationId, updateAuthPolicyRequestDto);
        return ResponseEntity.ok(authPolicyMapper.toDto(policy));
    }

}
