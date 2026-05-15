package com.asms.handler;

import com.asms.api.AccessControlApiDelegate;
import com.asms.model.AccessControlSimulateRequestDto;
import com.asms.model.AccessControlSimulateResponseDto;
import com.asms.model.EffectivePermissionsDto;
import com.asms.service.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the AccessControl API.
 *
 * <p>Implements {@link AccessControlApiDelegate}. Delegates all business logic to {@link AccessControlService}.
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
        return accessControlService.getEffectivePermissions(userId, organizationId);
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
        return accessControlService.simulateAccessControl(accessControlSimulateRequestDto);
    }
}
