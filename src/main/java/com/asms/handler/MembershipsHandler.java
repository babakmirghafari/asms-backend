package com.asms.handler;

import com.asms.api.MembershipsApiDelegate;
import com.asms.domain.Membership;
import com.asms.mapper.MembershipMapper;
import com.asms.model.CreateMembershipRequestDto;
import com.asms.model.MembershipDto;
import com.asms.model.PagedResponseDto;
import com.asms.service.MembershipsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Memberships API.
 *
 * <p>Implements {@link MembershipsApiDelegate}. Delegates all business logic to
 * {@link MembershipsService}. Maps domain {@link Membership} ↔ {@link MembershipDto}
 * via {@link MembershipMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipsHandler implements MembershipsApiDelegate {

    private final MembershipsService membershipsService;
    private final MembershipMapper membershipMapper;

    @Override
    public ResponseEntity<MembershipDto> createMembership(
            CreateMembershipRequestDto createMembershipRequestDto) {
        Membership membership = membershipsService.createMembership(createMembershipRequestDto);
        return ResponseEntity.status(201).body(membershipMapper.toDto(membership));
    }

    @Override
    public ResponseEntity<Void> deleteMembership(UUID membershipId) {
        membershipsService.deleteMembership(membershipId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MembershipDto> getMembershipById(UUID membershipId) {
        return ResponseEntity.ok(membershipMapper.toDto(membershipsService.getMembershipById(membershipId)));
    }

    @Override
    public ResponseEntity<PagedResponseDto> listMemberships(
            Integer page, Integer size, UUID organizationId, UUID userId) {
        Page<Membership> memberships =
                membershipsService.listMemberships(page, size, organizationId, userId);
        List<MembershipDto> dtos = memberships.getContent().stream().map(membershipMapper::toDto).toList();
        return ResponseEntity.ok(buildPage(dtos, memberships.getTotalElements(),
                memberships.getNumber(), memberships.getSize()));
    }

    // ─── private helpers ────────────────────────────────────────────────────

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
