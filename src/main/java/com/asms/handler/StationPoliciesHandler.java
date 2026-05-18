package com.asms.handler;

import com.asms.api.StationPoliciesApiDelegate;
import com.asms.domain.StationPolicy;
import com.asms.mapper.StationPolicyMapper;
import com.asms.model.CreateStationPolicyRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.StationPolicyDto;
import com.asms.model.UpdateStationPolicyRequestDto;
import com.asms.service.StationPoliciesService;
import com.asms.util.PageResponseBuilder;
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
 * {@link StationPoliciesService}. Maps domain {@link StationPolicy} ↔ {@link StationPolicyDto}
 * via {@link StationPolicyMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationPoliciesHandler implements StationPoliciesApiDelegate {

    private final StationPoliciesService stationPoliciesService;
    private final StationPolicyMapper stationPolicyMapper;

    @Override
    public ResponseEntity<StationPolicyDto> createStationPolicy(
            CreateStationPolicyRequestDto createStationPolicyRequestDto) {
        StationPolicy entity = stationPolicyMapper.toStationPolicyEntity(createStationPolicyRequestDto);
        StationPolicy policy = stationPoliciesService.createStationPolicy(entity);
        return ResponseEntity.status(201).body(stationPolicyMapper.toDto(policy));
    }

    @Override
    public ResponseEntity<Void> deleteStationPolicy(UUID policyId) {
        stationPoliciesService.deleteStationPolicy(policyId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StationPolicyDto> getStationPolicyById(UUID policyId) {
        return ResponseEntity.ok(stationPolicyMapper.toDto(stationPoliciesService.getStationPolicyById(policyId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listStationPolicies(
            Integer page, Integer size, UUID userId) {
        Page<StationPolicy> policies = stationPoliciesService.listStationPolicies(page, size, userId);
        List<StationPolicyDto> dtos = policies.getContent().stream().map(stationPolicyMapper::toDto).toList();
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, policies));
    }

    @Override
    public ResponseEntity<StationPolicyDto> updateStationPolicy(
            UUID policyId, UpdateStationPolicyRequestDto updateStationPolicyRequestDto) {
        StationPolicy patch = stationPolicyMapper.toStationPolicyPatch(updateStationPolicyRequestDto);
        StationPolicy policy = stationPoliciesService.updateStationPolicy(policyId, patch);
        return ResponseEntity.ok(stationPolicyMapper.toDto(policy));
    }

}
