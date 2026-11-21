package com.truckingsystem.service;

import com.truckingsystem.common.EmailAlreadyRegisteredException;
import com.truckingsystem.common.InvalidSignUpRequestException;
import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.entity.Company;
import com.truckingsystem.entity.CompanyMembership;
import com.truckingsystem.entity.CompanyStatus;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.CompanyRole;
import com.truckingsystem.entity.UserAccount;
import com.truckingsystem.entity.UserStatus;
import com.truckingsystem.pojo.SignUpRequest;
import com.truckingsystem.pojo.SignUpResponse;
import com.truckingsystem.repository.CompanyMembershipRepository;
import com.truckingsystem.repository.CompanyRepository;
import com.truckingsystem.repository.UserAccountRepository;
import com.truckingsystem.security.TenantDatabaseContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final TenantDatabaseContext tenantDatabaseContext;
    private final SupabaseAuthClient supabaseAuthClient;

    public AuthService(
            UserAccountRepository userAccountRepository,
            CompanyRepository companyRepository,
            CompanyMembershipRepository membershipRepository,
            TenantDatabaseContext tenantDatabaseContext,
            SupabaseAuthClient supabaseAuthClient) {
        this.userAccountRepository = userAccountRepository;
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.supabaseAuthClient = supabaseAuthClient;
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        SupabaseAuthClient.SupabaseUser identity = supabaseAuthClient.signUp(request);
        String subject = identity.subject();
        String email = identity.email().trim().toLowerCase(Locale.ROOT);

        UserAccount user = userAccountRepository.findByExternalSubject(subject)
                .or(() -> userAccountRepository.findByEmailIgnoreCase(email))
                .orElseGet(UserAccount::new);
        if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidSignUpRequestException("User account is not active");
        }
        if (user.getId() == null
                && userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        user.setExternalSubject(subject);
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setDisplayName(user.getFirstName() + " " + user.getLastName());
        user.setStatus(UserStatus.ACTIVE);
        user = userAccountRepository.saveAndFlush(user);

        boolean createCompany = request.companyName() != null && !request.companyName().isBlank();
        Company company;
        CompanyRole role;
        MemberStatus membershipStatus;

        if (createCompany) {
            company = new Company();
            company.setLegalName(request.companyName().trim());
            company.setDisplayName(request.companyName().trim());
            company.setCompanyCode(createCompanyCode(request.companyName()));
            company.setStatus(CompanyStatus.ACTIVE);
            company = companyRepository.save(company);
            role = CompanyRole.FLEET_MANAGER;
            membershipStatus = MemberStatus.ACTIVE;
        } else {
            company = companyRepository.findByIdAndStatus(request.existingCompanyId(), CompanyStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            role = CompanyRole.DISPATCHER;
            membershipStatus = MemberStatus.INVITED;
        }

        tenantDatabaseContext.activate(company.getId());
        CompanyMembership membership = new CompanyMembership();
        membership.setCompanyId(company.getId());
        membership.setUserId(user.getId());
        membership.setRole(role);
        membership.setStatus(membershipStatus);
        membership.setJoinedAt(membershipStatus == MemberStatus.ACTIVE ? Instant.now() : null);
        membership.setCreatedAt(Instant.now());
        membershipRepository.save(membership);

        return new SignUpResponse(
                user.getId(), company.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                role, membershipStatus, identity.accessToken());
    }

    private String createCompanyCode(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            normalized = "CARRIER";
        }
        String prefix = normalized.substring(0, Math.min(31, normalized.length())).replaceAll("-+$", "");
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
