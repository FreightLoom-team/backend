package com.truckingsystem.service;

import com.truckingsystem.common.InvalidMembershipStateException;
import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.common.TenantContext;
import com.truckingsystem.entity.CompanyMembership;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.CompanyRole;
import com.truckingsystem.repository.CompanyMembershipRepository;
import com.truckingsystem.security.TenantDatabaseContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyMembershipService {
    private static final List<CompanyRole> MEMBERSHIP_ADMIN_ROLES =
            List.of(CompanyRole.COMPANY_ADMIN, CompanyRole.FLEET_MANAGER);

    private final CompanyMembershipRepository memberRepository;
    private final TenantDatabaseContext tenantDatabaseContext;

    public CompanyMembershipService(
            CompanyMembershipRepository memberRepository, TenantDatabaseContext tenantDatabaseContext) {
        this.memberRepository = memberRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional
    public void activateInvitation(UUID membershipId) {
        TenantContext.Scope scope = TenantContext.current()
                .orElseThrow(() -> new IllegalStateException("Tenant scope is not established"));
        tenantDatabaseContext.activate(scope.companyId());
        boolean canManageMemberships = memberRepository
                .existsByCompanyIdAndUserIdAndRoleInAndStatus(
                        scope.companyId(), scope.userId(), MEMBERSHIP_ADMIN_ROLES, MemberStatus.ACTIVE);
        if (!canManageMemberships) {
            throw new AccessDeniedException("Only company administrators can activate memberships");
        }

        CompanyMembership member = memberRepository
                .findByIdAndCompanyId(membershipId, scope.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        if (member.getStatus() != MemberStatus.INVITED) {
            throw new InvalidMembershipStateException("Only invited memberships can be activated");
        }

        member.setStatus(MemberStatus.ACTIVE);
        member.setJoinedAt(java.time.Instant.now());
        memberRepository.save(member);
    }
}
