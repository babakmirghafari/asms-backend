package com.asms.handler;

import com.asms.api.MembershipsApiDelegate;
import com.asms.model.CreateMembershipRequestDto;
import com.asms.model.MembershipDto;
import com.asms.model.PagedResponseDto;
import com.asms.service.MembershipsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * REST adapter for the Memberships API.
 *
 * <p>Implements {@link MembershipsApiDelegate}. Delegates all business logic to {@link MembershipsService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipsHandler implements MembershipsApiDelegate {

    private final MembershipsService membershipsService;

    @Override
    public ResponseEntity<MembershipDto> createMembership(
            CreateMembershipRequestDto createMembershipRequestDto) {
        return membershipsService.createMembership(createMembershipRequestDto);
    }

    @Override
    public ResponseEntity<Void> deleteMembership(UUID membershipId) {
        return membershipsService.deleteMembership(membershipId);
    }

    @Override
    public ResponseEntity<MembershipDto> getMembershipById(UUID membershipId) {
        return membershipsService.getMembershipById(membershipId);
    }

    @Override
    public ResponseEntity<PagedResponseDto> listMemberships(
            Integer page, Integer size, UUID organizationId, UUID userId) {
        return membershipsService.listMemberships(page, size, organizationId, userId);
    }
}
