package com.truckingsystem.pojo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateLoadRequest(
        @NotBlank @Size(max = 50) String loadNumber,
        @Size(max = 160) String brokerName,
        @Size(max = 80) String brokerReference,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal rateAmount,
        @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @Size(max = 50) String equipmentType,
        @Size(max = 160) String commodity,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal weightLbs,
        Instant pickupDeadline,
        Instant deliveryDeadline) {
}
