package com.asms.handler;

import com.asms.api.AuthPoliciesApiDelegate;
import com.asms.domain.AuthPolicy;
import com.asms.mapper.AuthPolicyMapper;
import com.asms.model.AuthPolicyDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.UpdateAuthPolicyRequestDto;
import com.asms.service.AuthPoliciesService;
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
        return ResponseEntity.ok(buildPage(dtos, policies.getTotalElements(),
                policies.getNumber(), policies.getSize()));
    }

    @Override
    public ResponseEntity<AuthPolicyDto> updateAuthPolicy(
            UUID organizationId, UpdateAuthPolicyRequestDto updateAuthPolicyRequestDto) {
        AuthPolicy policy = authPoliciesService.updateAuthPolicy(organizationId, updateAuthPolicyRequestDto);
        return ResponseEntity.ok(authPolicyMapper.toDto(policy));
    }

    // ─── private helpers ────────────────────────────────────────────────────

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
