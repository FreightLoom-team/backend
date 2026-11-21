package com.truckingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id")
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @NotBlank
    @Size(max = 160)
    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_status", nullable = false, length = 20)
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @NotBlank
    @Size(max = 40)
    @Column(name = "company_code", nullable = false, unique = true, length = 40)
    private String companyCode;
}
