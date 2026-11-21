package com.truckingsystem.controller;

import com.truckingsystem.service.CompanyMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/company-memberships")
public class CompanyMembershipController {
    private final CompanyMembershipService membershipService;

    public CompanyMembershipController(CompanyMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PatchMapping("/{membershipId}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID membershipId) {
        membershipService.activateInvitation(membershipId);
        return ResponseEntity.noContent().build();
    }
}
