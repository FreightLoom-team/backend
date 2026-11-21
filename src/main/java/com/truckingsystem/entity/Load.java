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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loads", uniqueConstraints = {
        @UniqueConstraint(name = "loads_company_load_unique", columnNames = {"company_id", "load_id"}),
        @UniqueConstraint(name = "loads_company_number_unique", columnNames = {"company_id", "load_number"})
})
public class Load extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "load_id")
    private UUID id;

    @NotNull
    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "load_number", nullable = false, length = 50)
    private String loadNumber;

    @Size(max = 160)
    @Column(name = "broker_name", length = 160)
    private String brokerName;

    @Size(max = 80)
    @Column(name = "broker_reference", length = 80)
    private String brokerReference;

    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "rate_amount", precision = 12, scale = 2)
    private BigDecimal rateAmount;

    @NotBlank
    @Size(min = 3, max = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoadStatus status = LoadStatus.DRAFT;

    @Size(max = 50)
    @Column(name = "equipment_type", length = 50)
    private String equipmentType;

    @Size(max = 160)
    @Column(length = 160)
    private String commodity;

    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "weight_lbs", precision = 12, scale = 2)
    private BigDecimal weightLbs;

    @Column(name = "pickup_deadline")
    private Instant pickupDeadline;

    @Column(name = "delivery_deadline")
    private Instant deliveryDeadline;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "completed_at")
    private Instant completedAt;
}
