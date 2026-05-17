package com.asms.handler;

import com.asms.api.AccessControlApiDelegate;
import com.asms.model.AccessControlSimulateRequestDto;
import com.asms.model.AccessControlSimulateResponseDto;
import com.asms.model.EffectivePermissionsDto;
import com.asms.service.AccessControlService;
import com.asms.service.AccessControlService.EffectivePermissionsResult;
import com.asms.service.AccessControlService.SimulateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the AccessControl API.
 *
 * <p>Implements {@link AccessControlApiDelegate}. Delegates all business logic to
 * {@link AccessControlService}. Maps service result records to contract DTOs.
 *
 * <p>Note: {@code simulateAccessControl} is deprecated in v2.0.0 per the access-control delegate.
 * The v2 simulation endpoint is {@code POST /permissions/simulate} via {@link PermissionsHandler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessControlHandler implements AccessControlApiDelegate {

    private final AccessControlService accessControlService;

    @Override
    public ResponseEntity<EffectivePermissionsDto> getEffectivePermissions(
            UUID userId, UUID organizationId) {
        EffectivePermissionsResult result =
                accessControlService.getEffectivePermissions(userId, organizationId);
        EffectivePermissionsDto dto = new EffectivePermissionsDto();
        dto.setUserId(result.userId());
        dto.setOrganizationId(result.organizationId());
        dto.setPermissions(result.permissions());
        dto.setFromGroups(result.fromGroups());
        return ResponseEntity.ok(dto);
    }

    /**
     * @deprecated in v2.0.0 — use {@code POST /permissions/simulate} via
     *             {@link PermissionsHandler#simulatePermission} instead.
     *             Retained for one minor version per deprecation policy.
     *             Will be removed in v2.1.0.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    @Override
    public ResponseEntity<AccessControlSimulateResponseDto> simulateAccessControl(
            AccessControlSimulateRequestDto accessControlSimulateRequestDto) {
        SimulateResult result = accessControlService.simulateAccessControl(accessControlSimulateRequestDto);
        AccessControlSimulateResponseDto dto = new AccessControlSimulateResponseDto();
        dto.setDecision(result.granted()
                ? AccessControlSimulateResponseDto.DecisionEnum.ALLOW
                : AccessControlSimulateResponseDto.DecisionEnum.DENY);
        dto.setUserId(result.userId());
        dto.setOrganizationId(result.organizationId());
        dto.setResource(result.resource());
        dto.setAction(result.action());
        return ResponseEntity.ok(dto);
    }
}
