package com.asms.service;

import com.asms.domain.StationPolicy;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateStationPolicyRequestDto;
import com.asms.model.UpdateStationPolicyRequestDto;
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
    private final AuditService auditService;

    @Transactional
    public StationPolicy createStationPolicy(CreateStationPolicyRequestDto req) {
        UUID orgId = req.getOrganizationId() != null ? req.getOrganizationId() : TenantContext.getRequiredOrgId();
        StationPolicy policy = StationPolicy.builder()
                .orgId(orgId)
                .userId(null) // CreateStationPolicyRequestDto v2 has no userId; policies are org-scoped
                .name(req.getName())
                .description(req.getDescription())
                .status("ACTIVE")
                .allowedIps(req.getAllowedIpRanges())
                .allowedDays(req.getAllowedDays() != null
                        ? req.getAllowedDays().stream().map(Integer::shortValue).toList()
                        : null)
                .workHourStart(parseTime(req.getWorkStartTime()))
                .workHourEnd(parseTime(req.getWorkEndTime()))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
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
        return stationPolicyRepository.findByOrgId(
                orgId, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @Transactional
    public StationPolicy updateStationPolicy(UUID policyId, UpdateStationPolicyRequestDto req) {
        StationPolicy policy = loadPolicy(policyId);
        StationPolicy before = cloneForAudit(policy);
        if (req.getName() != null)           policy.setName(req.getName());
        if (req.getDescription() != null)    policy.setDescription(req.getDescription());
        if (req.getStatus() != null)         policy.setStatus(req.getStatus().getValue());
        if (req.getAllowedIpRanges() != null) policy.setAllowedIps(req.getAllowedIpRanges());
        if (req.getAllowedDays() != null)     policy.setAllowedDays(req.getAllowedDays().stream().map(Integer::shortValue).toList());
        if (req.getWorkStartTime() != null)  policy.setWorkHourStart(parseTime(req.getWorkStartTime()));
        if (req.getWorkEndTime() != null)    policy.setWorkHourEnd(parseTime(req.getWorkEndTime()));
        policy.setUpdatedAt(OffsetDateTime.now());
        StationPolicy saved = stationPolicyRepository.save(policy);
        auditService.recordInfo("STATION_POLICY", policyId, "STATION_POLICY_UPDATED", before, saved);
        return saved;
    }

    private StationPolicy loadPolicy(UUID policyId) {
        UUID orgId = TenantContext.getOrgId();
        if (orgId != null) {
            return stationPolicyRepository.findByOrgIdAndId(orgId, policyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Station policy not found: " + policyId));
        }
        return stationPolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Station policy not found: " + policyId));
    }

    /** Parses an HH:mm or HH:mm:ss time string to the hour component as Short. */
    public Short parseTime(String time) {
        if (time == null) return null;
        try {
            return Short.parseShort(time.split(":")[0]);
        } catch (NumberFormatException e) {
            log.warn("Could not parse time value: {}", time);
            return null;
        }
    }

    private StationPolicy cloneForAudit(StationPolicy p) {
        return StationPolicy.builder()
                .id(p.getId()).orgId(p.getOrgId()).userId(p.getUserId())
                .name(p.getName()).description(p.getDescription()).status(p.getStatus())
                .allowedIps(p.getAllowedIps()).allowedDays(p.getAllowedDays())
                .workHourStart(p.getWorkHourStart()).workHourEnd(p.getWorkHourEnd())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
                .build();
    }
}
