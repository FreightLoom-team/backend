package com.truckingsystem.pojo;

import com.truckingsystem.entity.OrganizationRole;
import com.truckingsystem.entity.MemberStatus;

import java.util.UUID;

public record SignUpResponse(
        UUID userId,
        UUID organizationId,
        String firstName,
        String lastName,
        String email,
        OrganizationRole role,
        MemberStatus membershipStatus) {
}
