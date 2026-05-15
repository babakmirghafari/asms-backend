package com.asms.service;

import com.asms.domain.Permission;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.AccessControlSimulateRequestDto;
import com.asms.model.AccessControlSimulateResponseDto;
import com.asms.model.EffectivePermissionsDto;
import com.asms.repository.UserRepository;
import com.asms.security.TenantContext;
import com.asms.service.EffectivePermissionService.EffectivePermissionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Access control and permission computation service (AC-11).
 *
 * <p>Delegates effective-permission computation to {@link EffectivePermissionService}.
 * Effective permissions = union of all group permissions; conflict detection flags
 * permissions sourced from multiple groups (union always applied — no explicit DENY
 * per ADR-001).
 *
 * <p>AC-13 org-scope enforcement: the requested organizationId must match the
 * caller's JWT org claim. Cross-tenant access returns 403.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final EffectivePermissionService effectivePermissionService;
    private final UserRepository userRepository;

    /**
     * Returns the effective permissions for a user in an org (AC-11).
     * Enforces org scope to prevent cross-tenant reads (AC-13).
     */
    public ResponseEntity<EffectivePermissionsDto> getEffectivePermissions(
            UUID userId, UUID organizationId) {
        log.debug("Compute effective permissions — user: {}, org: {}", userId, organizationId);

        // AC-13: validate requested org matches caller's org context
        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(organizationId)) {
            log.warn("Cross-tenant access attempt — caller org: {}, requested org: {}", callerOrg, organizationId);
            return ResponseEntity.status(403).build();
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        EffectivePermissionResult result = effectivePermissionService.compute(userId, organizationId);

        EffectivePermissionsDto dto = new EffectivePermissionsDto();
        dto.setUserId(userId);
        dto.setOrganizationId(organizationId);
        dto.setComputedAt(OffsetDateTime.now());

        // EffectivePermissionsDto.permissions is List<String> (permission names)
        List<String> permNames = result.permissions().stream()
                .map(Permission::getName)
                .toList();
        dto.setPermissions(permNames);

        // fromGroups = names of groups that contributed permissions
        List<String> groupNames = result.groups().stream()
                .map(g -> g.getName())
                .toList();
        dto.setFromGroups(groupNames);

        // direct = empty since all permissions flow through groups (ADR-001)
        dto.setDirect(List.of());

        return ResponseEntity.ok(dto);
    }

    /**
     * @deprecated in v2.0.0 — use {@code POST /permissions/simulate} via
     *             {@link PermissionsService#simulatePermission} instead.
     *             Will be removed in v2.1.0.
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public ResponseEntity<AccessControlSimulateResponseDto> simulateAccessControl(
            AccessControlSimulateRequestDto req) {
        log.warn("DEPRECATED: POST /access-control/simulate — migrate to POST /permissions/simulate");

        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(req.getOrganizationId())) {
            return ResponseEntity.status(403).build();
        }

        EffectivePermissionResult result = effectivePermissionService.compute(
                req.getUserId(), req.getOrganizationId());

        // v1 used resource + action; map to permission name format
        String permissionName = req.getResource() + "." + req.getAction();
        boolean granted = result.hasPermission(permissionName);

        AccessControlSimulateResponseDto response = new AccessControlSimulateResponseDto();
        response.setDecision(granted
                ? AccessControlSimulateResponseDto.DecisionEnum.ALLOW
                : AccessControlSimulateResponseDto.DecisionEnum.DENY);
        response.setUserId(req.getUserId());
        response.setOrganizationId(req.getOrganizationId());
        response.setResource(req.getResource());
        response.setAction(req.getAction() != null ? req.getAction().getValue() : null);
        response.setSimulatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(response);
    }
}
