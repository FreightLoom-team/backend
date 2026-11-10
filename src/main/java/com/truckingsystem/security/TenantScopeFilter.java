package com.truckingsystem.security;

import com.truckingsystem.common.TenantContext;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.OrganizationStatus;
import com.truckingsystem.entity.UserAccount;
import com.truckingsystem.repository.OrganizationMemberRepository;
import com.truckingsystem.repository.OrganizationRepository;
import com.truckingsystem.repository.UserAccountRepository;
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
    private static final String ORGANIZATION_CLAIM = "organization_id";

    private final UserAccountRepository userAccountRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;

    public TenantScopeFilter(
            UserAccountRepository userAccountRepository,
            OrganizationMemberRepository memberRepository,
            OrganizationRepository organizationRepository) {
        this.userAccountRepository = userAccountRepository;
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/actuator/health") || path.startsWith("/actuator/health/");
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

        UUID organizationId = parseOrganizationId(jwtAuthentication, response);
        if (organizationId == null) {
            return;
        }

        UserAccount user = userAccountRepository
                .findByExternalSubjectAndActiveTrue(jwtAuthentication.getToken().getSubject())
                .orElse(null);
        if (user == null
                || !memberRepository.existsByOrganizationIdAndUserIdAndStatus(
                        organizationId, user.getId(), MemberStatus.ACTIVE)
                || organizationRepository.findByIdAndStatus(organizationId, OrganizationStatus.ACTIVE).isEmpty()) {
            writeForbidden(response);
            return;
        }

        TenantContext.set(organizationId, user.getId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID parseOrganizationId(JwtAuthenticationToken authentication, HttpServletResponse response)
            throws IOException {
        Object claim = authentication.getToken().getClaims().get(ORGANIZATION_CLAIM);
        if (!(claim instanceof String organizationIdClaim)) {
            writeForbidden(response);
            return null;
        }
        try {
            return UUID.fromString(organizationIdClaim);
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
