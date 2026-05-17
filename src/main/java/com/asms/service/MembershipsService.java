package com.asms.service;

import com.asms.domain.Membership;
import com.asms.exception.AccessDeniedException;
import com.asms.exception.ResourceNotFoundException;
import com.asms.model.CreateMembershipRequestDto;
import com.asms.repository.MembershipRepository;
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
 * User-org membership management service (AC-3, AC-12, AC-13).
 *
 * <p>Returns domain entities — never {@code ResponseEntity}. HTTP wrapping is done
 * in {@link com.asms.handler.MembershipsHandler}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipsService {

    private final MembershipRepository membershipRepository;
    private final AuditService auditService;

    @Transactional
    public Membership createMembership(CreateMembershipRequestDto req) {
        Membership membership = Membership.builder()
                .userId(req.getUserId())
                .orgId(req.getOrganizationId())
                .role("MEMBER")
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        Membership saved = membershipRepository.save(membership);
        auditService.recordInfo("MEMBERSHIP", saved.getId(), "MEMBERSHIP_CREATED", null, saved);
        return saved;
    }

    @Transactional
    public void deleteMembership(UUID membershipId) {
        Membership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membership not found: " + membershipId));
        // AC-13: ensure the membership belongs to caller's org
        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(m.getOrgId())) {
            throw new AccessDeniedException("Cannot delete membership from a different organization");
        }
        m.setStatus("REMOVED");
        m.setUpdatedAt(OffsetDateTime.now());
        membershipRepository.save(m);
        auditService.recordInfo("MEMBERSHIP", membershipId, "MEMBERSHIP_REMOVED", null, m);
    }

    @Transactional(readOnly = true)
    public Membership getMembershipById(UUID membershipId) {
        Membership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membership not found: " + membershipId));
        // AC-13: org scope validation
        UUID callerOrg = TenantContext.getOrgId();
        if (callerOrg != null && !callerOrg.equals(m.getOrgId())) {
            throw new AccessDeniedException("Cannot access membership from a different organization");
        }
        return m;
    }

    @Transactional(readOnly = true)
    public Page<Membership> listMemberships(
            Integer page, Integer size, UUID organizationId, UUID userId) {
        return membershipRepository.findFiltered(
                organizationId, userId,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }
}
