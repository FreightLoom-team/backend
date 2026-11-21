package com.truckingsystem.service;

import com.truckingsystem.common.EmailAlreadyRegisteredException;
import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.entity.Company;
import com.truckingsystem.entity.CompanyStatus;
import com.truckingsystem.entity.CompanyMembership;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.CompanyRole;
import com.truckingsystem.entity.UserAccount;
import com.truckingsystem.entity.UserStatus;
import com.truckingsystem.pojo.SignUpRequest;
import com.truckingsystem.repository.CompanyMembershipRepository;
import com.truckingsystem.repository.CompanyRepository;
import com.truckingsystem.repository.UserAccountRepository;
import com.truckingsystem.security.TenantDatabaseContext;
import org.junit.jupiter.api.Test;
import com.truckingsystem.service.SupabaseAuthClient.SupabaseUser;
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
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final CompanyMembershipRepository memberships = mock(CompanyMembershipRepository.class);
    private final TenantDatabaseContext tenantDatabaseContext = mock(TenantDatabaseContext.class);
    private final SupabaseAuthClient supabaseAuthClient = mock(SupabaseAuthClient.class);
    private final AuthService service =
            new AuthService(users, companies, memberships, tenantDatabaseContext, supabaseAuthClient);

    @Test
    void createsCompanyAndFleetManagerMembershipFromVerifiedSupabaseIdentity() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        givenSupabaseSignup("driver@example.com", "supabase-user", "test-access-token");
        when(users.findByExternalSubject("supabase-user")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("driver@example.com")).thenReturn(Optional.empty());
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
        when(companies.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setId(companyId);
            return company;
        });
        when(memberships.save(any(CompanyMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.signUp(new SignUpRequest(
                "driver@example.com", "strong-password", "Casey", "Driver", "Haul Co.", null));

        assertEquals(userId, response.userId());
        assertEquals(companyId, response.companyId());
        assertEquals(CompanyRole.FLEET_MANAGER, response.role());
        assertEquals("test-access-token", response.accessToken());

        var userCaptor = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(userCaptor.capture());
        assertEquals("driver@example.com", userCaptor.getValue().getEmail());
        assertEquals("supabase-user", userCaptor.getValue().getExternalSubject());
        assertEquals(UserStatus.ACTIVE, userCaptor.getValue().getStatus());

        var companyCaptor = org.mockito.ArgumentCaptor.forClass(Company.class);
        verify(companies).save(companyCaptor.capture());
        assertEquals("Haul Co.", companyCaptor.getValue().getLegalName());
        assertEquals(CompanyStatus.ACTIVE, companyCaptor.getValue().getStatus());

        var membershipCaptor = org.mockito.ArgumentCaptor.forClass(CompanyMembership.class);
        verify(memberships).save(membershipCaptor.capture());
        assertEquals(userId, membershipCaptor.getValue().getUserId());
        assertEquals(companyId, membershipCaptor.getValue().getCompanyId());
        assertEquals(MemberStatus.ACTIVE, membershipCaptor.getValue().getStatus());
    }

    @Test
    void joinsExistingCompanyAsInvitedDispatcher() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        givenSupabaseSignup("driver@example.com", "supabase-user", null);
        Company company = new Company();
        company.setId(companyId);
        when(users.findByExternalSubject("supabase-user")).thenReturn(Optional.empty());
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
        when(companies.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).thenReturn(Optional.of(company));
        when(memberships.save(any(CompanyMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.signUp(new SignUpRequest(
                "driver@example.com", "strong-password", "Casey", "Driver", null, companyId));

        assertEquals(companyId, response.companyId());
        assertEquals(CompanyRole.DISPATCHER, response.role());
        assertEquals(MemberStatus.INVITED, response.membershipStatus());
        var membershipCaptor = org.mockito.ArgumentCaptor.forClass(CompanyMembership.class);
        verify(memberships).save(membershipCaptor.capture());
        assertEquals(MemberStatus.INVITED, membershipCaptor.getValue().getStatus());
        verify(companies, never()).save(any(Company.class));
    }

    @Test
    void rejectsDuplicateEmailWithoutCreatingLocalCredentials() {
        givenSupabaseSignup("driver@example.com", "supabase-user", null);
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.signUp(
                new SignUpRequest("driver@example.com", "strong-password", "Casey", "Driver", "Haul Co.", null)));

        verify(users, never()).saveAndFlush(any(UserAccount.class));
    }

    @Test
    void rejectsUnknownCompany() {
        UUID companyId = UUID.randomUUID();
        givenSupabaseSignup("driver@example.com", "supabase-user", null);
        when(users.findByExternalSubject("supabase-user")).thenReturn(Optional.empty());
        when(users.existsByEmailIgnoreCase("driver@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(companies.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.signUp(
                new SignUpRequest("driver@example.com", "strong-password", "Casey", "Driver", null, companyId)));
        verify(memberships, never()).save(any(CompanyMembership.class));
    }

    private void givenSupabaseSignup(String email, String subject, String accessToken) {
        when(supabaseAuthClient.signUp(any()))
                .thenReturn(new SupabaseUser(subject, email, accessToken));
    }
}
