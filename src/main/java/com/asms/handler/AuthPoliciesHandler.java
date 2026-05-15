package com.asms.handler;

import com.asms.api.AuthPoliciesApiDelegate;
import com.asms.model.AuthPolicyDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateAuthPolicyRequestDto;
import com.asms.service.AuthPoliciesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the AuthPolicies API.
 *
 * <p>Implements {@link AuthPoliciesApiDelegate}. Delegates all business logic to {@link AuthPoliciesService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPoliciesHandler implements AuthPoliciesApiDelegate {

    private final AuthPoliciesService authPoliciesService;

    @Override
    public ResponseEntity<AuthPolicyDto> getAuthPolicyByOrganization(UUID organizationId) {
        return authPoliciesService.getAuthPolicyByOrganization(organizationId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listAuthPolicies(Integer page, Integer size) {
        return authPoliciesService.listAuthPolicies(page, size);
    }

    @Override
    public ResponseEntity<AuthPolicyDto> updateAuthPolicy(
            UUID organizationId, UpdateAuthPolicyRequestDto updateAuthPolicyRequestDto) {
        return authPoliciesService.updateAuthPolicy(organizationId, updateAuthPolicyRequestDto);
    }
}
