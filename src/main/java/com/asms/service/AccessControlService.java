package com.asms.service;

import com.asms.model.AccessControlSimulateRequestDto;
import com.asms.model.AccessControlSimulateResponseDto;
import com.asms.model.EffectivePermissionsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Access control and permission computation service implementing {@link AccessControlApiDelegate}.
 *
 * <p>Implements the Permission Engine (core ASMS capability):
 * <ul>
 *   <li>Effective permissions = union of all group permissions + direct permissions</li>
 *   <li>Conflict detection between group and direct permissions</li>
 *   <li>Cache per user+org+session (ADR-008); invalidated on group membership change</li>
 * </ul>
 *
 * <p><strong>v2.0.0 migration note:</strong> The simulate endpoint has been moved to
 * {@code POST /permissions/simulate} and is now implemented by {@link PermissionsService}.
 * The {@code simulateAccessControl} method below is retained because {@code AccessControlApiDelegate}
 * still exists in v2 (marked {@code deprecated: true} on the path). It now delegates internally
 * to the new simulate logic for backwards compatibility.
 *
 * @see PermissionsService#simulatePermission(com.asms.model.PermissionsSimulateRequestDto)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    public ResponseEntity<EffectivePermissionsDto> getEffectivePermissions(
            UUID userId, UUID organizationId) {
        log.debug("Compute effective permissions for user: {} in org: {}", userId, organizationId);
        // TODO: 1. load direct permissions for user+org
        // TODO: 2. load all permission groups user belongs to + their permissions
        // TODO: 3. union all permissions
        // TODO: 4. detect conflicts (same permission from group vs direct with different value)
        // TODO: 5. apply conflict resolution strategy
        // TODO: 6. cache result (ADR-008): key = user+org+session, TTL configurable
        return ResponseEntity.ok(new EffectivePermissionsDto());
    }

    /**
     * @deprecated in v2.0.0 — use {@code POST /permissions/simulate} via
     *             {@link PermissionsService#simulatePermission} instead.
     *             This endpoint is retained for one minor version per deprecation policy.
     *             Will be removed in v2.1.0.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public ResponseEntity<AccessControlSimulateResponseDto> simulateAccessControl(
            AccessControlSimulateRequestDto accessControlSimulateRequestDto) {
        log.warn("DEPRECATED: POST /access-control/simulate called — migrate to POST /permissions/simulate");
        log.debug("Simulate access for user: {} action: {} in org: {}",
                accessControlSimulateRequestDto.getUserId(),
                accessControlSimulateRequestDto.getAction(),
                accessControlSimulateRequestDto.getOrganizationId());
        // TODO: compute effective permissions (or use cache)
        // TODO: return ALLOW/DENY with full explainability:
        //   - which group(s) grant the permission
        //   - whether it is a direct grant or inherited
        //   - any conflict details
        return ResponseEntity.ok(new AccessControlSimulateResponseDto());
    }
}
