package com.truckingsystem.service;

import com.truckingsystem.common.TenantContext;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.OrganizationMember;
import com.truckingsystem.entity.OrganizationRole;
import com.truckingsystem.repository.OrganizationMemberRepository;
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

class OrganizationMembershipServiceTest {
    private final OrganizationMemberRepository members = mock(OrganizationMemberRepository.class);
    private final OrganizationMembershipService service = new OrganizationMembershipService(members);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void activatesInvitationOnlyWithinVerifiedTenantForAdmin() {
        UUID organizationId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID invitedId = UUID.randomUUID();
        TenantContext.set(organizationId, adminId);
        OrganizationMember invitation = new OrganizationMember();
        invitation.setId(invitedId);
        invitation.setOrganizationId(organizationId);
        invitation.setStatus(MemberStatus.INVITED);

        when(members.existsByOrganizationIdAndUserIdAndRoleInAndStatus(
                eq(organizationId), eq(adminId), any(), eq(MemberStatus.ACTIVE))).thenReturn(true);
        when(members.findByIdAndOrganizationId(invitedId, organizationId))
                .thenReturn(Optional.of(invitation));
        when(members.save(invitation)).thenReturn(invitation);

        service.activateInvitation(invitedId);

        assertEquals(MemberStatus.ACTIVE, invitation.getStatus());
        verify(members).findByIdAndOrganizationId(invitedId, organizationId);
        verify(members).save(invitation);
    }

    @Test
    void deniesNonAdminBeforeLookingUpMembership() {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID invitedId = UUID.randomUUID();
        TenantContext.set(organizationId, userId);
        when(members.existsByOrganizationIdAndUserIdAndRoleInAndStatus(
                eq(organizationId), eq(userId), any(), eq(MemberStatus.ACTIVE))).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.activateInvitation(invitedId));

        verify(members, never()).findByIdAndOrganizationId(invitedId, organizationId);
        verify(members, never()).save(any(OrganizationMember.class));
    }
}
