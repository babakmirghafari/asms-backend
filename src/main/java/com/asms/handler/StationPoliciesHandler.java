package com.asms.handler;

import com.asms.api.StationPoliciesApiDelegate;
import com.asms.model.CreateStationPolicyRequestDto;
import com.asms.model.PagedResponseDto;
import com.asms.model.StationPolicyDto;
import com.asms.model.UpdateStationPolicyRequestDto;
import com.asms.service.StationPoliciesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the StationPolicies API.
 *
 * <p>Implements {@link StationPoliciesApiDelegate}. Delegates all business logic to {@link StationPoliciesService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationPoliciesHandler implements StationPoliciesApiDelegate {

    private final StationPoliciesService stationPoliciesService;

    @Override
    public ResponseEntity<StationPolicyDto> createStationPolicy(
            CreateStationPolicyRequestDto createStationPolicyRequestDto) {
        return stationPoliciesService.createStationPolicy(createStationPolicyRequestDto);
    }

    @Override
    public ResponseEntity<Void> deleteStationPolicy(UUID policyId) {
        return stationPoliciesService.deleteStationPolicy(policyId);
    }

    @Override
    public ResponseEntity<StationPolicyDto> getStationPolicyById(UUID policyId) {
        return stationPoliciesService.getStationPolicyById(policyId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listStationPolicies(
            Integer page, Integer size, UUID userId) {
        return stationPoliciesService.listStationPolicies(page, size, userId);
    }

    @Override
    public ResponseEntity<StationPolicyDto> updateStationPolicy(
            UUID policyId, UpdateStationPolicyRequestDto updateStationPolicyRequestDto) {
        return stationPoliciesService.updateStationPolicy(policyId, updateStationPolicyRequestDto);
    }
}
