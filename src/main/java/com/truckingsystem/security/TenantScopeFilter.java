package com.truckingsystem.security;

import com.truckingsystem.common.TenantContext;
import com.truckingsystem.service.TenantAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantScopeFilter extends OncePerRequestFilter {
    private static final String COMPANY_CLAIM = "company_id";

    private final TenantAccessService tenantAccessService;

    public TenantScopeFilter(TenantAccessService tenantAccessService) {
        this.tenantAccessService = tenantAccessService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/")
                || path.equals("/api/v1/auth/signup");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID companyId = parseCompanyId(jwtAuthentication, response);
        if (companyId == null) {
            return;
        }

        UUID userId = tenantAccessService.findActiveUserIdForCompany(
                jwtAuthentication.getToken().getSubject(), companyId).orElse(null);
        if (userId == null) {
            writeForbidden(response);
            return;
        }

        TenantContext.set(companyId, userId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID parseCompanyId(JwtAuthenticationToken authentication, HttpServletResponse response)
            throws IOException {
        Object claim = authentication.getToken().getClaims().get(COMPANY_CLAIM);
        if (!(claim instanceof String companyIdClaim)) {
            writeForbidden(response);
            return null;
        }
        try {
            return UUID.fromString(companyIdClaim);
        } catch (IllegalArgumentException exception) {
            writeForbidden(response);
            return null;
        }
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"title":"Tenant access denied","status":403}
                """);
    }
}
