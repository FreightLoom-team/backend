package com.truckingsystem.security;

import com.truckingsystem.common.TenantContext;
import com.truckingsystem.service.TenantAccessService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantScopeFilterTest {
    private final TenantAccessService tenantAccessService = mock(TenantAccessService.class);
    private final TenantScopeFilter filter = new TenantScopeFilter(tenantAccessService);

    @AfterEach
    void clearSecurityState() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void scopesAuthenticatedRequestToVerifiedTokenCompanyAndClearsContext() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(tenantAccessService.findActiveUserIdForCompany("oidc-user", companyId))
                .thenReturn(Optional.of(userId));
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(companyId), List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Company-Id", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};

        filter.doFilter(request, response, (req, res) -> {
            chainInvoked[0] = true;
            TenantContext.Scope scope = TenantContext.current().orElseThrow();
            assertEquals(companyId, scope.companyId());
            assertEquals(userId, scope.userId());
        });

        assertTrue(chainInvoked[0]);
        assertNull(TenantContext.current().orElse(null));
        verify(tenantAccessService).findActiveUserIdForCompany("oidc-user", companyId);
    }

    @Test
    void rejectsCompanyNotBelongingToAuthenticatedUser() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(tenantAccessService.findActiveUserIdForCompany("oidc-user", companyId))
                .thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(companyId), List.of()));

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainInvoked = {false};
        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> chainInvoked[0] = true);

        assertFalse(chainInvoked[0]);
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Tenant access denied"));
        assertTrue(TenantContext.current().isEmpty());
    }

    private Jwt jwt(UUID companyId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "ES256")
                .subject("oidc-user")
                .claim("company_id", companyId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
