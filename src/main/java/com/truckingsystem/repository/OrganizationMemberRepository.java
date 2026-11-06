package com.truckingsystem.repository;

import com.truckingsystem.entity.MemberStatus;
import com.truckingsystem.entity.OrganizationMember;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface OrganizationMemberRepository extends Repository<OrganizationMember, UUID> {
    boolean existsByOrganizationIdAndUserIdAndStatus(
            UUID organizationId, UUID userId, MemberStatus status);
}
