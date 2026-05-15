package com.asms.service;

import com.asms.domain.Membership;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateMembershipRequestDto;
import com.asms.model.MembershipDto;
import com.asms.model.PagedResponseDto;
import com.asms.repository.MembershipRepository;
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
 * User-org membership management service (AC-3, AC-12, AC-13).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipsService {

    private final MembershipRepository membershipRepository;
    private final AuditService auditService;

    @Transactional
    public ResponseEntity<MembershipDto> createMembership(CreateMembershipRequestDto req) {
        Membership membership = Membership.builder()
                .userId(req.getUserId())
                .orgId(req.getOrganizationId())
                .role("MEMBER")
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        Membership saved = membershipRepository.save(membership);
        auditService.recordInfo("MEMBERSHIP", saved.getId(), "MEMBERSHIP_CREATED", null, toDto(saved));
        return ResponseEntity.status(201).body(toDto(saved));
    }

    @Transactional
    public ResponseEntity<Void> deleteMembership(UUID membershipId) {
        Membership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + membershipId));
        // AC-13: ensure the membership belongs to caller's org
        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(m.getOrgId())) {
            return ResponseEntity.status(403).build();
        }
        MembershipDto before = toDto(m);
        m.setStatus("REMOVED");
        m.setUpdatedAt(OffsetDateTime.now());
        membershipRepository.save(m);
        auditService.recordInfo("MEMBERSHIP", membershipId, "MEMBERSHIP_REMOVED", before, toDto(m));
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<MembershipDto> getMembershipById(UUID membershipId) {
        Membership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found: " + membershipId));
        // AC-13: org scope validation
        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(m.getOrgId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(toDto(m));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponseDto> listMemberships(
            Integer page, Integer size, UUID organizationId, UUID userId) {
        Page<Membership> results = membershipRepository.findFiltered(
                organizationId, userId,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
        return ResponseEntity.ok(buildPage(
                results.getContent().stream().map(this::toDto).toList(),
                results.getTotalElements(), results.getNumber(), results.getSize()));
    }

    private MembershipDto toDto(Membership m) {
        MembershipDto dto = new MembershipDto();
        dto.setId(m.getId());
        dto.setUserId(m.getUserId());
        dto.setOrganizationId(m.getOrgId());
        dto.setStatus(MembershipDto.StatusEnum.fromValue(m.getStatus()));
        dto.setCreatedAt(m.getCreatedAt());
        dto.setUpdatedAt(m.getUpdatedAt());
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
