package com.truckingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_memberships_company_user", columnNames = {"company_id", "user_id"})
})
public class CompanyMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "membership_id")
    private UUID id;

    @NotNull
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 24)
    private CompanyRole role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    @Column(name = "joined_at")
    private java.time.Instant joinedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;
}
