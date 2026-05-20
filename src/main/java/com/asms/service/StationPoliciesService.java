package com.asms.service;

import com.asms.domain.Organization;
import com.asms.domain.StationPolicy;
import com.asms.exception.ResourceNotFoundException;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.StationPolicyRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Station policy management service (AC-3, AC-12, AC-13).
 * StationPolicyDto (v2.0.0): id, name, description, organizationId, status,
 * allowedIpRanges, allowedDays, workStartTime, workEndTime, createdAt, updatedAt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationPoliciesService {

    private final StationPolicyRepository stationPolicyRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    /**
     * Persists a new station policy. The handler converts the request DTO to a
     * {@link StationPolicy} entity via the mapper before calling here.
     */
    @Transactional
    public StationPolicy createStationPolicy(StationPolicy policy, UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        policy.setOrganization(org);
        StationPolicy saved = stationPolicyRepository.save(policy);
        auditService.recordInfo("STATION_POLICY", saved.getId(), "STATION_POLICY_CREATED", null, saved);
        return saved;
    }

    @Transactional
    public void deleteStationPolicy(UUID policyId) {
        StationPolicy policy = loadPolicy(policyId);
        stationPolicyRepository.delete(policy);
        auditService.recordInfo("STATION_POLICY", policyId, "STATION_POLICY_DELETED", policy, null);
    }

    @Transactional(readOnly = true)
    public StationPolicy getStationPolicyById(UUID policyId) {
        return loadPolicy(policyId);
    }

    @Transactional(readOnly = true)
    public Page<StationPolicy> listStationPolicies(Integer page, Integer size, UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        return stationPolicyRepository.findByOrganizationId(
                orgId, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    /**
     * Applies updates to an existing station policy.
     * The handler extracts primitive fields from the request DTO before calling here.
     *
     * @param policyId      target policy identifier
     * @param patch         partial StationPolicy carrying the fields to update (null fields not applied)
     */
    @Transactional
    public StationPolicy updateStationPolicy(UUID policyId, StationPolicy patch) {
        StationPolicy policy = loadPolicy(policyId);
        StationPolicy before = cloneForAudit(policy);
        if (patch.getName() != null)           policy.setName(patch.getName());
        if (patch.getDescription() != null)    policy.setDescription(patch.getDescription());
        if (patch.getStatus() != null)         policy.setStatus(patch.getStatus());
        if (patch.getAllowedIps() != null)      policy.setAllowedIps(patch.getAllowedIps());
        if (patch.getAllowedDays() != null)     policy.setAllowedDays(patch.getAllowedDays());
        if (patch.getWorkHourStart() != null)  policy.setWorkHourStart(patch.getWorkHourStart());
        if (patch.getWorkHourEnd() != null)    policy.setWorkHourEnd(patch.getWorkHourEnd());
        policy.setUpdatedAt(OffsetDateTime.now());
        StationPolicy saved = stationPolicyRepository.save(policy);
        auditService.recordInfo("STATION_POLICY", policyId, "STATION_POLICY_UPDATED", before, saved);
        return saved;
    }

    private StationPolicy loadPolicy(UUID policyId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return stationPolicyRepository.findByOrganizationIdAndId(orgId, policyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Station policy not found: " + policyId));
        }
        return stationPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Station policy not found: " + policyId));
    }

    private StationPolicy cloneForAudit(StationPolicy p) {
        return StationPolicy.builder()
                .id(p.getId()).organization(p.getOrganization()).user(p.getUser())
                .name(p.getName()).description(p.getDescription()).status(p.getStatus())
                .allowedIps(p.getAllowedIps()).allowedDays(p.getAllowedDays())
                .workHourStart(p.getWorkHourStart()).workHourEnd(p.getWorkHourEnd())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
