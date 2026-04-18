package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiatePaymentRequest {

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotNull
    @Positive(message = "Amount must be positive")
    private Double amount;

    private String currency = "INR";

    private String description;
}
