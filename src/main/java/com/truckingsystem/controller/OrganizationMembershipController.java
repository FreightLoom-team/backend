package com.truckingsystem.controller;

import com.truckingsystem.service.OrganizationMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organization-members")
public class OrganizationMembershipController {
    private final OrganizationMembershipService membershipService;

    public OrganizationMembershipController(OrganizationMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PatchMapping("/{membershipId}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID membershipId) {
        membershipService.activateInvitation(membershipId);
        return ResponseEntity.noContent().build();
    }
}
