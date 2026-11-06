package com.truckingsystem.security;

import com.truckingsystem.common.TenantContext;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.Organization;
import com.truckingsystem.entity.OrganizationStatus;
import com.truckingsystem.entity.UserAccount;
import com.truckingsystem.repository.OrganizationMemberRepository;
import com.truckingsystem.repository.OrganizationRepository;
import com.truckingsystem.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantScopeFilterTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final OrganizationMemberRepository members = mock(OrganizationMemberRepository.class);
    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final TenantScopeFilter filter = new TenantScopeFilter(users, members, organizations);

    @AfterEach
    void clearSecurityState() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void scopesAuthenticatedRequestToVerifiedTokenOrganizationAndClearsContext() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(userId);
        Organization organization = new Organization();
        organization.setId(organizationId);

        when(users.findByExternalSubjectAndActiveTrue("oidc-user"))
                .thenReturn(Optional.of(user));
        when(members.existsByOrganizationIdAndUserIdAndStatus(
                organizationId, userId, MemberStatus.ACTIVE)).thenReturn(true);
        when(organizations.findByIdAndStatus(organizationId, OrganizationStatus.ACTIVE))
                .thenReturn(Optional.of(organization));
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(organizationId), List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Organization-Id", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (req, res) -> {
            chainInvoked[0] = true;
            TenantContext.Scope scope = TenantContext.current().orElseThrow();
            assertEquals(organizationId, scope.organizationId());
            assertEquals(userId, scope.userId());
        });

        assertTrue(chainInvoked[0]);
        assertNull(TenantContext.current().orElse(null));
        verify(members).existsByOrganizationIdAndUserIdAndStatus(
                eq(organizationId), eq(userId), eq(MemberStatus.ACTIVE));
    }

    @Test
    void rejectsOrganizationNotBelongingToAuthenticatedUser() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(userId);

        when(users.findByExternalSubjectAndActiveTrue("oidc-user"))
                .thenReturn(Optional.of(user));
        when(members.existsByOrganizationIdAndUserIdAndStatus(
                organizationId, userId, MemberStatus.ACTIVE)).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(organizationId), List.of()));

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};
        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> chainInvoked[0] = true);

        assertFalse(chainInvoked[0]);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Tenant access denied"));
        assertTrue(TenantContext.current().isEmpty());
        verify(organizations, never()).findByIdAndStatus(organizationId, OrganizationStatus.ACTIVE);
    }

    private Jwt jwt(UUID organizationId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("oidc-user")
                .claim("organization_id", organizationId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
