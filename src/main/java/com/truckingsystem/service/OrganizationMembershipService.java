package com.truckingsystem.service;

import com.truckingsystem.common.InvalidMembershipStateException;
import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.common.TenantContext;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.OrganizationMember;
import com.truckingsystem.entity.OrganizationRole;
import com.truckingsystem.repository.OrganizationMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationMembershipService {
    private static final List<OrganizationRole> MEMBERSHIP_ADMIN_ROLES =
            List.of(OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.FLEET_MANAGER);

    private final OrganizationMemberRepository memberRepository;

    public OrganizationMembershipService(OrganizationMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void activateInvitation(UUID membershipId) {
        TenantContext.Scope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Tenant scope is not established"));
        boolean canManageMemberships = memberRepository
                .existsByOrganizationIdAndUserIdAndRoleInAndStatus(
                        scope.organizationId(), scope.userId(), MEMBERSHIP_ADMIN_ROLES, MemberStatus.ACTIVE);
        if (!canManageMemberships) {
            throw new AccessDeniedException("Only organization administrators can activate memberships");
        }

        OrganizationMember member = memberRepository
                .findByIdAndOrganizationId(membershipId, scope.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        if (member.getStatus() != MemberStatus.INVITED) {
            throw new InvalidMembershipStateException("Only invited memberships can be activated");
        }

        member.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);
    }
}
