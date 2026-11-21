package com.truckingsystem.service;

import com.truckingsystem.common.TenantContext;
import com.truckingsystem.entity.CompanyMembership;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.repository.CompanyMembershipRepository;
import com.truckingsystem.security.TenantDatabaseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyMembershipServiceTest {
    private final CompanyMembershipRepository memberships = mock(CompanyMembershipRepository.class);
    private final TenantDatabaseContext tenantDatabaseContext = mock(TenantDatabaseContext.class);
    private final CompanyMembershipService service =
            new CompanyMembershipService(memberships, tenantDatabaseContext);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void activatesInvitationOnlyWithinVerifiedCompanyForAdmin() {
        UUID companyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID invitedId = UUID.randomUUID();
        TenantContext.set(companyId, adminId);
        CompanyMembership invitation = new CompanyMembership();
        invitation.setId(invitedId);
        invitation.setCompanyId(companyId);
        invitation.setStatus(MemberStatus.INVITED);

        when(memberships.existsByCompanyIdAndUserIdAndRoleInAndStatus(
                eq(companyId), eq(adminId), any(), eq(MemberStatus.ACTIVE))).thenReturn(true);
        when(memberships.findByIdAndCompanyId(invitedId, companyId))
                .thenReturn(Optional.of(invitation));
        when(memberships.save(invitation)).thenReturn(invitation);

        service.activateInvitation(invitedId);

        assertEquals(MemberStatus.ACTIVE, invitation.getStatus());
        verify(memberships).findByIdAndCompanyId(invitedId, companyId);
        verify(memberships).save(invitation);
    }

    @Test
    void deniesNonAdminBeforeLookingUpMembership() {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID invitedId = UUID.randomUUID();
        TenantContext.set(companyId, userId);
        when(memberships.existsByCompanyIdAndUserIdAndRoleInAndStatus(
                eq(companyId), eq(userId), any(), eq(MemberStatus.ACTIVE))).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.activateInvitation(invitedId));

        verify(memberships, never()).findByIdAndCompanyId(invitedId, companyId);
        verify(memberships, never()).save(any(CompanyMembership.class));
    }
}
