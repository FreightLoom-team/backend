package com.truckingsystem.service;

import com.truckingsystem.common.EmailAlreadyRegisteredException;
import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.Organization;
import com.truckingsystem.entity.OrganizationMember;
import com.truckingsystem.entity.OrganizationRole;
import com.truckingsystem.entity.OrganizationStatus;
import com.truckingsystem.entity.UserAccount;
import com.truckingsystem.pojo.SignUpRequest;
import com.truckingsystem.repository.OrganizationMemberRepository;
import com.truckingsystem.repository.OrganizationRepository;
import com.truckingsystem.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final OrganizationMemberRepository members = mock(OrganizationMemberRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthService service = new AuthService(users, organizations, members, passwordEncoder);

    @Test
    void createsCarrierAndFleetManagerMembershipWithEncodedPassword() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
        when(organizations.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization organization = invocation.getArgument(0);
            organization.setId(organizationId);
            return organization;
        });
        when(members.save(any(OrganizationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.signUp(new SignUpRequest(
                "Casey", "Driver", " DRIVER@example.com ", "strong-password", "Haul Co.", null));

        assertEquals(userId, response.userId());
        assertEquals(organizationId, response.organizationId());
        assertEquals(OrganizationRole.FLEET_MANAGER, response.role());

        var userCaptor = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(userCaptor.capture());
        assertEquals("driver@example.com", userCaptor.getValue().getEmail());
        assertEquals("bcrypt-hash", userCaptor.getValue().getPasswordHash());
        verify(passwordEncoder).encode("strong-password");

        var organizationCaptor = org.mockito.ArgumentCaptor.forClass(Organization.class);
        verify(organizations).save(organizationCaptor.capture());
        assertEquals("Haul Co.", organizationCaptor.getValue().getName());
        assertEquals(OrganizationStatus.ACTIVE, organizationCaptor.getValue().getStatus());

        var memberCaptor = org.mockito.ArgumentCaptor.forClass(OrganizationMember.class);
        verify(members).save(memberCaptor.capture());
        assertEquals(userId, memberCaptor.getValue().getUserId());
        assertEquals(organizationId, memberCaptor.getValue().getOrganizationId());
        assertEquals(OrganizationRole.FLEET_MANAGER, memberCaptor.getValue().getRole());
        assertEquals(MemberStatus.ACTIVE, memberCaptor.getValue().getStatus());
    }

    @Test
    void joinsExistingCarrierAsDispatcher() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(organizationId);
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(organizations.findByIdAndStatus(organizationId, OrganizationStatus.ACTIVE))
                .thenReturn(Optional.of(organization));
        when(members.save(any(OrganizationMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.signUp(new SignUpRequest(
                "Casey", "Driver", "driver@example.com", "strong-password", null, organizationId));

        assertEquals(organizationId, response.organizationId());
        assertEquals(OrganizationRole.DISPATCHER, response.role());
        assertEquals(MemberStatus.INVITED, response.membershipStatus());
        var memberCaptor = org.mockito.ArgumentCaptor.forClass(OrganizationMember.class);
        verify(members).save(memberCaptor.capture());
        assertEquals(MemberStatus.INVITED, memberCaptor.getValue().getStatus());
        verify(organizations, never()).save(any(Organization.class));
    }

    @Test
    void rejectsDuplicateEmailBeforeSaving() {
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.signUp(new SignUpRequest(
                "Casey", "Driver", "driver@example.com", "strong-password", "Haul Co.", null)));

        verify(users, never()).saveAndFlush(any(UserAccount.class));
    }

    @Test
    void rejectsUnknownOrganization() {
        UUID organizationId = UUID.randomUUID();
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(organizations.findByIdAndStatus(organizationId, OrganizationStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.signUp(new SignUpRequest(
                "Casey", "Driver", "driver@example.com", "strong-password", null, organizationId)));
        verify(members, never()).save(any(OrganizationMember.class));
    }
}
