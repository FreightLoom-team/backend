package com.truckingsystem.repository;

import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.OrganizationMember;
import com.truckingsystem.entity.OrganizationRole;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends Repository<OrganizationMember, UUID> {
    OrganizationMember save(OrganizationMember member);

    boolean existsByOrganizationIdAndUserIdAndStatus(
            UUID organizationId, UUID userId, MemberStatus status);

    boolean existsByOrganizationIdAndUserIdAndRoleInAndStatus(
            UUID organizationId, UUID userId, Collection<OrganizationRole> roles, MemberStatus status);

    Optional<OrganizationMember> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
