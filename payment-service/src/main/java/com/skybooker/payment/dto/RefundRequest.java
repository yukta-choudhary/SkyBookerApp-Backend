package com.skybooker.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class RefundRequest {

    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    @Positive(message = "Refund amount must be positive")
    private Double refundAmount; // null = full refund

    private String reason;
}
