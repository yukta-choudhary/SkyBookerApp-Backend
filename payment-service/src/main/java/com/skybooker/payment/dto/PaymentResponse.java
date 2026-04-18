package com.skybooker.payment.dto;

import com.skybooker.payment.enums.PaymentMode;
import com.skybooker.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMode paymentMode;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private Double refundAmount;
    private LocalDateTime createdAt;

    // Returned only on initiation — for frontend Razorpay checkout modal
    private String razorpayKeyId;
}
