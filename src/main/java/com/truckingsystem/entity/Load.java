package com.truckingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loads")
public class Load extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String reference;

    @NotBlank
    @Size(max = 200)
    @Column(name = "pickup_location", nullable = false, length = 200)
    private String pickupLocation;

    @NotBlank
    @Size(max = 200)
    @Column(name = "delivery_location", nullable = false, length = 200)
    private String deliveryLocation;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 15, fraction = 4)
    @Column(name = "rate_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rateAmount;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LoadStatus status = LoadStatus.DRAFT;
}
