package com.asms.service;

import com.asms.domain.Permission;
import com.asms.domain.PermissionGroup;
import com.asms.repository.PermissionGroupRepository;
import com.asms.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Permission Engine — computes effective permissions for a user in an organization (AC-11).
 *
 * <p>Per ADR-008 and §8.1:
 * <ul>
 *   <li>Effective permissions = union of all group permissions + any direct permissions assigned to the user</li>
 *   <li>Conflict detection: if the same permission name is found via a group AND via direct assignment
 *       with different representations (should not occur in this model since permissions are unified by
 *       their identity), a conflict is flagged in the result.</li>
 *   <li>There are no explicit DENY records in this model (ADR-001 — role-free, no explicit deny).
 *       Conflict detection here flags duplicate permission sources, not DENY vs ALLOW.</li>
 * </ul>
 *
 * <p>Redis caching (ADR-008) is deferred until the Redis dependency is wired.
 * For v1, this service computes permissions fresh from the database on each call.
 * The in-memory computation is fast enough for the current scale requirement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EffectivePermissionService {

    private final PermissionGroupRepository permissionGroupRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Computes the effective permission set for a user within an organisation.
     *
     * @param userId the user to evaluate
     * @param orgId  the organisation context
     * @return result containing the effective permissions, source map, and any conflicts
     */
    @Transactional(readOnly = true)
    public EffectivePermissionResult compute(UUID userId, UUID orgId) {
        log.debug("Computing effective permissions — user: {}, org: {}", userId, orgId);

        // 1. Load all groups the user belongs to in this org
        List<PermissionGroup> groups = permissionGroupRepository.findGroupsForUser(userId, orgId);

        // 2. Collect all group-sourced permissions and track their source group
        Map<UUID, Set<String>> permissionSources = new HashMap<>(); // permissionId → set of source labels
        Set<Permission> groupPermissions = new HashSet<>();

        for (PermissionGroup group : groups) {
            for (Permission perm : group.getPermissions()) {
                if ("ACTIVE".equals(perm.getStatus())) {
                    groupPermissions.add(perm);
                    permissionSources
                            .computeIfAbsent(perm.getId(), k -> new HashSet<>())
                            .add("group:" + group.getName());
                }
            }
        }

        // 3. No separate "direct permissions" table exists in the current schema —
        //    permissions are granted exclusively through group membership (ADR-001).
        //    This is the correct model: ASMS has NO roles and NO direct permission assignments.
        //    All permissions flow through PermissionGroups.
        Set<Permission> allPermissions = groupPermissions;

        // 4. Conflict detection: flag any permission that is sourced from more than one group
        List<PermissionConflict> conflicts = new ArrayList<>();
        for (Map.Entry<UUID, Set<String>> entry : permissionSources.entrySet()) {
            if (entry.getValue().size() > 1) {
                Permission perm = allPermissions.stream()
                        .filter(p -> p.getId().equals(entry.getKey()))
                        .findFirst()
                        .orElse(null);
                if (perm != null) {
                    conflicts.add(new PermissionConflict(
                            perm.getId(),
                            perm.getName(),
                            new ArrayList<>(entry.getValue()),
                            "Permission granted by multiple groups — union applied, no conflict in effective access"
                    ));
                }
            }
        }

        log.debug("Effective permissions computed — user: {}, org: {}, count: {}, conflicts: {}",
                userId, orgId, allPermissions.size(), conflicts.size());

        return new EffectivePermissionResult(userId, orgId, allPermissions, groups, permissionSources, conflicts);
    }

    // ─── Result types ────────────────────────────────────────────────────────

    public record EffectivePermissionResult(
            UUID userId,
            UUID orgId,
            Set<Permission> permissions,
            List<PermissionGroup> groups,
            Map<UUID, Set<String>> permissionSources,
            List<PermissionConflict> conflicts
    ) {
        public boolean hasPermission(String permissionName) {
            return permissions.stream().anyMatch(p -> p.getName().equals(permissionName));
        }
    }

    public record PermissionConflict(
            UUID permissionId,
            String permissionName,
            List<String> sources,
            String resolution
    ) {}
}
