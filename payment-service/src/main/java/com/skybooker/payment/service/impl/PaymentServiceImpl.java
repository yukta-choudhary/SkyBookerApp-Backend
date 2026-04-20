package com.skybooker.payment.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.skybooker.payment.dto.*;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.enums.PaymentStatus;
import com.skybooker.payment.event.PaymentSuccessEvent;
import com.skybooker.payment.repository.PaymentRepository;
import com.skybooker.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    @Value("${services.booking-url}")
    private String bookingServiceUrl;

    @Override
    public PaymentResponse initiatePayment(InitiatePaymentRequest request, UUID userId) {
        try {
            // Check if payment already initiated for this booking
            paymentRepository.findByBookingId(request.getBookingId()).ifPresent(existing -> {
                if (existing.getStatus() == PaymentStatus.PAID) {
                    throw new RuntimeException("Booking already paid");
                }
            });

            // Create Razorpay order (amount in paise = amount * 100)
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (long) (request.getAmount() * 100));
            orderRequest.put("currency", request.getCurrency() != null ? request.getCurrency() : defaultCurrency);
            orderRequest.put("receipt", "rcpt_" + request.getBookingId().toString().replace("-", "").substring(0, 10));
            orderRequest.put("payment_capture", 1);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            // Persist payment record
            Payment payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .userId(userId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : defaultCurrency)
                    .status(PaymentStatus.PENDING)
                    .razorpayOrderId(orderId)
                    .build();

            payment = paymentRepository.save(payment);

            PaymentResponse response = toResponse(payment);
            response.setRazorpayKeyId(razorpayKeyId); // Sent to frontend for checkout modal
            return response;

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse verifyAndConfirmPayment(PaymentVerificationRequest request) {
        // 1. Verify Razorpay HMAC signature
        if (!verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature())) {
            throw new RuntimeException("Payment verification failed: invalid signature");
        }

        // 2. Find payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment record not found for order: " + request.getRazorpayOrderId()));

        // 3. Update payment record
        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        // 4. Confirm booking via REST call to booking-service
        try {
            restTemplate.put(
                    bookingServiceUrl + "/api/v1/bookings/" + payment.getBookingId() + "/confirm",
                    null
            );
            log.info("Booking {} confirmed after successful payment", payment.getBookingId());
        } catch (Exception e) {
            log.error("Failed to confirm booking {}: {}", payment.getBookingId(), e.getMessage());
            // Don't fail payment — booking will be confirmed via retry/manual
        }

        // 5. Publish Kafka event for notification-service (non-blocking — log warning if Kafka unavailable)
        try {
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .paymentId(payment.getPaymentId())
                    .bookingId(payment.getBookingId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .razorpayPaymentId(request.getRazorpayPaymentId())
                    .build();
            kafkaTemplate.send("payment-success", payment.getBookingId().toString(), event);
            log.info("Published payment-success event for booking {}", payment.getBookingId());
        } catch (Exception e) {
            log.warn("Could not publish payment-success Kafka event (Kafka may be unavailable): {}", e.getMessage());
            // Payment is still successful — Kafka is optional for local dev
        }

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        return toResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(UUID bookingId) {
        return toResponse(paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for booking: " + bookingId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(UUID userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse refundPayment(RefundRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + request.getPaymentId()));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Only paid payments can be refunded");
        }

        try {
            double refundAmt = request.getRefundAmount() != null ? request.getRefundAmount() : payment.getAmount();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", (long) (refundAmt * 100)); // paise

            Refund refund = razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundRequest);

            payment.setStatus(refundAmt >= payment.getAmount() ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
            payment.setRefundAmount(refundAmt);
            payment.setRefundedAt(LocalDateTime.now());
            payment.setRefundId(refund.get("id"));
            payment = paymentRepository.save(payment);

            log.info("Refund {} processed for payment {}", refund.get("id"), payment.getPaymentId());
            return toResponse(payment);

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        return paymentRepository.getTotalRevenue();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);
            return generated.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .paymentMode(p.getPaymentMode())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .transactionId(p.getTransactionId())
                .paidAt(p.getPaidAt())
                .refundedAt(p.getRefundedAt())
                .refundAmount(p.getRefundAmount())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
