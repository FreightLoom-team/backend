package com.truckingsystem.service;

import com.truckingsystem.entity.CompanyStatus;
import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.UserStatus;
import com.truckingsystem.repository.CompanyMembershipRepository;
import com.truckingsystem.repository.CompanyRepository;
import com.truckingsystem.repository.UserAccountRepository;
import com.truckingsystem.security.TenantDatabaseContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TenantAccessService {
    private final TenantDatabaseContext tenantDatabaseContext;
    private final UserAccountRepository userAccountRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final CompanyRepository companyRepository;

    public TenantAccessService(
            TenantDatabaseContext tenantDatabaseContext,
            UserAccountRepository userAccountRepository,
            CompanyMembershipRepository membershipRepository,
            CompanyRepository companyRepository) {
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findActiveUserIdForCompany(String externalSubject, UUID companyId) {
        var user = userAccountRepository
                .findByExternalSubjectAndStatus(externalSubject, UserStatus.ACTIVE);
        if (user.isEmpty() || companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE).isEmpty()) {
            return Optional.empty();
        }

        tenantDatabaseContext.activate(companyId);

        boolean activeMember = membershipRepository.existsByCompanyIdAndUserIdAndStatus(
                companyId, user.get().getId(), MemberStatus.ACTIVE);
        return activeMember ? Optional.of(user.get().getId()) : Optional.empty();
    }
}
