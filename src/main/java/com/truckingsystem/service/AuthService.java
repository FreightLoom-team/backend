package com.truckingsystem.service;

import com.truckingsystem.common.EmailAlreadyRegisteredException;
import com.truckingsystem.common.InvalidSignUpRequestException;
import com.truckingsystem.common.ResourceNotFoundException;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.Organization;
import com.truckingsystem.entity.OrganizationMember;
import com.truckingsystem.entity.OrganizationRole;
import com.truckingsystem.entity.OrganizationStatus;
import com.truckingsystem.entity.UserAccount;
import com.truckingsystem.pojo.SignUpRequest;
import com.truckingsystem.pojo.SignUpResponse;
import com.truckingsystem.repository.OrganizationMemberRepository;
import com.truckingsystem.repository.OrganizationRepository;
import com.truckingsystem.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        boolean hasCompanyName = request.companyName() != null && !request.companyName().isBlank();
        boolean hasOrganizationId = request.existingOrganizationId() != null;
        if (hasCompanyName == hasOrganizationId) {
            throw new InvalidSignUpRequestException(
                    "Provide either a company name or an existing organization ID, but not both");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidSignUpRequestException("Password must not exceed 72 UTF-8 bytes");
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        UserAccount user = new UserAccount();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setDisplayName(user.getFirstName() + " " + user.getLastName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        user = userAccountRepository.saveAndFlush(user);

        Organization organization;
        OrganizationRole role;
        MemberStatus membershipStatus;
        if (hasCompanyName) {
            organization = new Organization();
            organization.setName(request.companyName().trim());
            organization.setSlug(createSlug(request.companyName()));
            organization.setStatus(OrganizationStatus.ACTIVE);
            organization.setCreatedBy(user.getId());
            organization.setUpdatedBy(user.getId());
            organization = organizationRepository.save(organization);
            role = OrganizationRole.FLEET_MANAGER;
            membershipStatus = MemberStatus.ACTIVE;
        } else {
            organization = organizationRepository
                    .findByIdAndStatus(request.existingOrganizationId(), OrganizationStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            role = OrganizationRole.DISPATCHER;
            membershipStatus = MemberStatus.INVITED;
        }

        OrganizationMember member = new OrganizationMember();
        member.setOrganizationId(organization.getId());
        member.setUserId(user.getId());
        member.setRole(role);
        member.setStatus(membershipStatus);
        member.setCreatedBy(user.getId());
        member.setUpdatedBy(user.getId());
        organizationMemberRepository.save(member);

        return new SignUpResponse(
                user.getId(), organization.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                role, membershipStatus);
    }

    private String createSlug(String companyName) {
        String normalized = Normalizer.normalize(companyName, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            normalized = "freight-carrier";
        }
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String prefix = normalized.substring(0, Math.min(normalized.length(), 70)).replaceAll("-+$", "");
        return prefix + "-" + suffix;
    }
}
