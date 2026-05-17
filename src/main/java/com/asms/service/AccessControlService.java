package com.asms.service;

import com.asms.domain.Permission;
import com.asms.exception.AccessDeniedException;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AccessControlSimulateRequestDto;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import com.asms.service.EffectivePermissionService.EffectivePermissionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Access control and permission computation service (AC-11).
 *
 * <p>Returns plain records — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.AccessControlHandler}.
 *
 * <p>Delegates effective-permission computation to {@link EffectivePermissionService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final EffectivePermissionService effectivePermissionService;
    private final UserRepository userRepository;

    /**
     * Result of effective permissions computation.
     */
    public record EffectivePermissionsResult(UUID userId, UUID organizationId,
                                              List<String> permissions, List<String> fromGroups) {}

    /**
     * Result of an access control simulation.
     */
    public record SimulateResult(boolean granted, UUID userId, UUID organizationId,
                                  String resource, String action) {}

    /**
     * Returns the effective permissions for a user in an org (AC-11).
     * Enforces org scope to prevent cross-tenant reads (AC-13).
     */
    public EffectivePermissionsResult getEffectivePermissions(UUID userId, UUID organizationId) {
        log.debug("Compute effective permissions — user: {}, org: {}", userId, organizationId);

        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(organizationId)) {
            log.warn("Cross-tenant access attempt — caller org: {}, requested org: {}",
                    callerOrg, organizationId);
            throw new AccessDeniedException("Cross-tenant access denied");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        EffectivePermissionResult result = effectivePermissionService.compute(userId, organizationId);

        List<String> permNames = result.permissions().stream()
                .map(Permission::getName).toList();
        List<String> groupNames = result.groups().stream()
                .map(g -> g.getName()).toList();

        return new EffectivePermissionsResult(userId, organizationId, permNames, groupNames);
    }

    /**
     * @deprecated in v2.0.0 — use {@code POST /permissions/simulate} via
     *             {@link PermissionsService#simulatePermission} instead.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public SimulateResult simulateAccessControl(AccessControlSimulateRequestDto req) {
        log.warn("DEPRECATED: POST /access-control/simulate — migrate to POST /permissions/simulate");

        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(req.getOrganizationId())) {
            throw new AccessDeniedException("Cross-tenant access denied");
        }

        EffectivePermissionResult result = effectivePermissionService.compute(
                req.getUserId(), req.getOrganizationId());

        String permissionName = req.getResource() + "." + req.getAction();
        boolean granted = result.hasPermission(permissionName);

        return new SimulateResult(granted, req.getUserId(), req.getOrganizationId(),
                req.getResource(), req.getAction() != null ? req.getAction().getValue() : null);
    }
}
