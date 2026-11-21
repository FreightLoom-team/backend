package com.truckingsystem.repository;

import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.CompanyMembership;
import com.truckingsystem.entity.CompanyRole;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CompanyMembershipRepository extends Repository<CompanyMembership, UUID> {
    CompanyMembership save(CompanyMembership member);

    boolean existsByCompanyIdAndUserIdAndStatus(
            UUID companyId, UUID userId, MemberStatus status);

    boolean existsByCompanyIdAndUserIdAndRoleInAndStatus(
            UUID companyId, UUID userId, Collection<CompanyRole> roles, MemberStatus status);

    Optional<CompanyMembership> findByIdAndCompanyId(UUID id, UUID companyId);
}
