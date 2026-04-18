package com.skybooker.payment.service;

import com.skybooker.payment.dto.InitiatePaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.dto.PaymentVerificationRequest;
import com.skybooker.payment.dto.RefundRequest;
import com.skybooker.payment.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse initiatePayment(InitiatePaymentRequest request, UUID userId);

    PaymentResponse verifyAndConfirmPayment(PaymentVerificationRequest request);

    PaymentResponse getPaymentById(UUID paymentId);

    PaymentResponse getPaymentByBookingId(UUID bookingId);

    List<PaymentResponse> getPaymentsByUser(UUID userId);

    PaymentResponse refundPayment(RefundRequest request);

    Double getTotalRevenue();
}
