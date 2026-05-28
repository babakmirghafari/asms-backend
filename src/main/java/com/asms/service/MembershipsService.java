package com.asms.service;

import com.asms.domain.Membership;
import com.asms.domain.Organization;
import com.asms.domain.User;
import com.asms.domain.enums.MembershipStatus;
import com.asms.exception.AccessDeniedException;
import com.asms.exception.ConflictException;
import com.asms.exception.ResourceNotFoundException;
import com.asms.repository.MembershipRepository;
import com.asms.repository.OrganizationRepository;
import com.asms.repository.UserRepository;
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
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    /**
     * Persists a new membership. The handler converts the request DTO to a
     * {@link Membership} entity via the mapper before calling here.
     */
    @Transactional
    public Membership createMembership(Membership membership, UUID orgId, UUID userId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        java.util.Optional<Membership> existing = membershipRepository.findByUserIdAndOrganizationId(userId, orgId);
        if (existing.isPresent()) {
            Membership m = existing.get();
            if (m.getStatus() != MembershipStatus.REMOVED) {
                throw new ConflictException("This user is already a member of this organization.");
            }
            // Reactivate the soft-deleted row instead of inserting a duplicate
            // (unique constraint on user_id + org_id would block a new insert).
            m.setStatus(MembershipStatus.ACTIVE);
            m.setUpdatedAt(OffsetDateTime.now());
            Membership saved = membershipRepository.save(m);
            auditService.recordInfo("MEMBERSHIP", saved.getId(), "MEMBERSHIP_REACTIVATED", null, saved);
            return saved;
        }

        membership.setOrganization(org);
        membership.setUser(user);
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
        if (callerOrg != null && !callerOrg.equals(m.getOrganization().getId())) {
            throw new AccessDeniedException("Cannot delete membership from a different organization");
        }
        m.setStatus(MembershipStatus.REMOVED);
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
        if (callerOrg != null && !callerOrg.equals(m.getOrganization().getId())) {
            throw new AccessDeniedException("Cannot access membership from a different organization");
        }
        return m;
    }

    @Transactional(readOnly = true)
    public Page<Membership> listMemberships(
            Integer page, Integer size, UUID organizationId, UUID userId) {
        return membershipRepository.findFiltered(
                organizationId, userId, MembershipStatus.REMOVED,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }
}
