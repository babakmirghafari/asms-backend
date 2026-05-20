package com.asms.handler;

import com.asms.api.MembershipsApiDelegate;
import com.asms.domain.Membership;
import com.asms.domain.Organization;
import com.asms.domain.User;
import com.asms.exception.ResourceNotFoundException;
import com.asms.mapper.MembershipMapper;
import com.asms.model.CreateMembershipRequestDto;
import com.asms.model.MembershipDto;
import com.asms.model.PagedResponseDto;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.UserRepository;
import com.asms.service.MembershipsService;
import com.asms.util.PageResponseBuilder;
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
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<MembershipDto> createMembership(
            CreateMembershipRequestDto createMembershipRequestDto) {
        Membership entity = membershipMapper.toMembershipEntity(createMembershipRequestDto);
        User user = userRepository.findById(createMembershipRequestDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + createMembershipRequestDto.getUserId()));
        Organization org = organizationRepository.findById(createMembershipRequestDto.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found: " + createMembershipRequestDto.getOrganizationId()));
        entity.setUser(user);
        entity.setOrganization(org);
        Membership membership = membershipsService.createMembership(entity);
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
        return ResponseEntity.ok(PageResponseBuilder.build(dtos, memberships));
    }

}
