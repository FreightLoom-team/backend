package com.truckingsystem.pojo;

import com.truckingsystem.entity.CompanyRole;
import com.truckingsystem.entity.MemberStatus;

import java.util.UUID;

public record SignUpResponse(
        UUID userId,
        UUID companyId,
        String firstName,
        String lastName,
        String email,
        CompanyRole role,
        MemberStatus membershipStatus,
        String accessToken) {
}
