package com.asms.service;

import com.asms.domain.StationPolicy;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateStationPolicyRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.StationPolicyDto;
import com.asms.model.UpdateStationPolicyRequestDto;
import com.asms.repository.StationPolicyRepository;
import com.asms.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
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
    public ResponseEntity<StationPolicyDto> createStationPolicy(CreateStationPolicyRequestDto req) {
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
        auditService.recordInfo("STATION_POLICY", saved.getId(), "STATION_POLICY_CREATED", null, toDto(saved));
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Transactional
    public ResponseEntity<Void> deleteStationPolicy(UUID policyId) {
        StationPolicy policy = loadPolicy(policyId);
        StationPolicyDto before = toDto(policy);
        stationPolicyRepository.delete(policy);
        auditService.recordInfo("STATION_POLICY", policyId, "STATION_POLICY_DELETED", before, null);
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StationPolicyDto> getStationPolicyById(UUID policyId) {
        return ResponseEntity.ok(toDto(loadPolicy(policyId)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listStationPolicies(Integer page, Integer size, UUID organizationId) {
        UUID orgId = organizationId != null ? organizationId : TenantContext.getRequiredOrgId();
        Page<StationPolicy> results = stationPolicyRepository.findByOrgId(
                orgId, PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    @Transactional
    public ResponseEntity<StationPolicyDto> updateStationPolicy(
            UUID policyId, UpdateStationPolicyRequestDto req) {
        StationPolicy policy = loadPolicy(policyId);
        StationPolicyDto before = toDto(policy);
        if (req.getName() != null)           policy.setName(req.getName());
        if (req.getDescription() != null)    policy.setDescription(req.getDescription());
        if (req.getStatus() != null)         policy.setStatus(req.getStatus().getValue());
        if (req.getAllowedIpRanges() != null) policy.setAllowedIps(req.getAllowedIpRanges());
        if (req.getAllowedDays() != null)     policy.setAllowedDays(req.getAllowedDays().stream().map(Integer::shortValue).toList());
        if (req.getWorkStartTime() != null)  policy.setWorkHourStart(parseTime(req.getWorkStartTime()));
        if (req.getWorkEndTime() != null)    policy.setWorkHourEnd(parseTime(req.getWorkEndTime()));
        policy.setUpdatedAt(OffsetDateTime.now());
        StationPolicy saved = stationPolicyRepository.save(policy);
        auditService.recordInfo("STATION_POLICY", policyId, "STATION_POLICY_UPDATED", before, toDto(saved));
        return ResponseEntity.ok(toDto(saved));
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

    private StationPolicyDto toDto(StationPolicy p) {
        StationPolicyDto dto = new StationPolicyDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setOrganizationId(p.getOrgId());
        if (p.getStatus() != null) {
            dto.setStatus(StationPolicyDto.StatusEnum.fromValue(p.getStatus()));
        }
        // StationPolicyDto uses allowedIpRanges; domain uses allowedIps
        dto.setAllowedIpRanges(p.getAllowedIps());
        dto.setAllowedDays(p.getAllowedDays() != null
                ? p.getAllowedDays().stream().map(Short::intValue).toList() : null);
        // StationPolicyDto uses workStartTime/workEndTime (String); domain uses workHourStart/End (Short)
        dto.setWorkStartTime(p.getWorkHourStart() != null ? p.getWorkHourStart() + ":00" : null);
        dto.setWorkEndTime(p.getWorkHourEnd() != null ? p.getWorkHourEnd() + ":00" : null);
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    /** Parses an HH:mm or HH:mm:ss time string to the hour component as Short. */
    private Short parseTime(String time) {
        if (time == null) return null;
        try {
            return Short.parseShort(time.split(":")[0]);
        } catch (NumberFormatException e) {
            log.warn("Could not parse time value: {}", time);
            return null;
        }
    }

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
