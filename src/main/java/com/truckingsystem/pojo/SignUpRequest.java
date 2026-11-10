package com.truckingsystem.pojo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SignUpRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 200) String companyName,
        UUID existingOrganizationId) {

    @AssertTrue(message = "Provide either a company name or an existing organization ID, but not both")
    public boolean hasExactlyOneOrganizationOption() {
        boolean hasCompanyName = companyName != null && !companyName.isBlank();
        return hasCompanyName ^ (existingOrganizationId != null);
    }
}
