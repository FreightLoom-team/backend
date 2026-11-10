package com.truckingsystem.pojo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLoadRequest(
        @NotBlank @Size(max = 200) String reference,
        @NotBlank @Size(max = 200) String pickupLocation,
        @NotBlank @Size(max = 200) String deliveryLocation,
        @NotNull @DecimalMin("0.00") @Digits(integer = 15, fraction = 4) BigDecimal rateAmount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currencyCode) {
}
