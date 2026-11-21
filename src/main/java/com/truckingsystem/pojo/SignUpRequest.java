package com.truckingsystem.pojo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SignUpRequest(
        @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Size(max = 160) String companyName,
        UUID existingCompanyId) {

    @AssertTrue(message = "Provide either a company name or an existing company ID, but not both")
    public boolean hasExactlyOneCompanyOption() {
        boolean hasCompanyName = companyName != null && !companyName.isBlank();
        return hasCompanyName ^ (existingCompanyId != null);
    }
}
