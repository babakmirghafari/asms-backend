package com.asms.handler;

import com.asms.api.StationPoliciesApiDelegate;
import com.asms.domain.StationPolicy;
import com.asms.model.CreateStationPolicyRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.StationPolicyDto;
import com.asms.model.UpdateStationPolicyRequestDto;
import com.asms.service.StationPoliciesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the StationPolicies API.
 *
 * <p>Implements {@link StationPoliciesApiDelegate}. Delegates all business logic to
 * {@link StationPoliciesService}. Maps domain {@link StationPolicy} to {@link StationPolicyDto}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationPoliciesHandler implements StationPoliciesApiDelegate {

    private final StationPoliciesService stationPoliciesService;

    @Override
    public ResponseEntity<StationPolicyDto> createStationPolicy(
            CreateStationPolicyRequestDto createStationPolicyRequestDto) {
        StationPolicy policy = stationPoliciesService.createStationPolicy(createStationPolicyRequestDto);
        return ResponseEntity.status(201).body(toDto(policy));
    }

    @Override
    public ResponseEntity<Void> deleteStationPolicy(UUID policyId) {
        stationPoliciesService.deleteStationPolicy(policyId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StationPolicyDto> getStationPolicyById(UUID policyId) {
        return ResponseEntity.ok(toDto(stationPoliciesService.getStationPolicyById(policyId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listStationPolicies(
            Integer page, Integer size, UUID userId) {
        Page<StationPolicy> policies = stationPoliciesService.listStationPolicies(page, size, userId);
        List<StationPolicyDto> dtos = policies.getContent().stream().map(this::toDto).toList();
        return ResponseEntity.ok(buildPage(dtos, policies.getTotalElements(),
                policies.getNumber(), policies.getSize()));
    }

    @Override
    public ResponseEntity<StationPolicyDto> updateStationPolicy(
            UUID policyId, UpdateStationPolicyRequestDto updateStationPolicyRequestDto) {
        StationPolicy policy = stationPoliciesService.updateStationPolicy(policyId, updateStationPolicyRequestDto);
        return ResponseEntity.ok(toDto(policy));
    }

    // ─── private helpers ────────────────────────────────────────────────────

    private StationPolicyDto toDto(StationPolicy p) {
        StationPolicyDto dto = new StationPolicyDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setOrganizationId(p.getOrgId());
        if (p.getStatus() != null) {
            dto.setStatus(StationPolicyDto.StatusEnum.fromValue(p.getStatus()));
        }
        dto.setAllowedIpRanges(p.getAllowedIps());
        dto.setAllowedDays(p.getAllowedDays() != null
                ? p.getAllowedDays().stream().map(Short::intValue).toList() : null);
        dto.setWorkStartTime(p.getWorkHourStart() != null ? p.getWorkHourStart() + ":00" : null);
        dto.setWorkEndTime(p.getWorkHourEnd() != null ? p.getWorkHourEnd() + ":00" : null);
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
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
