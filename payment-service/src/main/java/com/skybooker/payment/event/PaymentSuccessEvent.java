package com.skybooker.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private String pnrCode;
    private String userEmail;
    private String userFullName;
    private Double amount;
    private String currency;
    private String razorpayPaymentId;
    private String flightNumber;
    private String origin;
    private String destination;
}
