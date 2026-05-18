package com.skybooker.payment.controller;

import com.skybooker.payment.config.JwtService;
import com.skybooker.payment.dto.*;
import com.skybooker.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Razorpay payment initiation, verification, refund, and payment history APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtService jwtService;

    @Operation(
        summary = "Initiate Payment (Step 1)",
        description = "Creates a Razorpay order for the given booking. Returns orderId, amount, currency, and razorpayKeyId needed for the frontend Razorpay checkout modal."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Razorpay order created successfully"),
        @ApiResponse(responseCode = "400", description = "Booking already paid or invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden — PASSENGER role required")
    })
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<PaymentResponse> initiate(
            @Valid @RequestBody InitiatePaymentRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiatePayment(request, userId));
    }

    @Operation(
        summary = "Verify Payment (Step 2)",
        description = "Verifies the Razorpay HMAC signature after the checkout modal succeeds. Confirms the booking and publishes a payment-success Kafka event."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment verified and booking confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid Razorpay signature"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/verify")
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<PaymentResponse> verify(@Valid @RequestBody PaymentVerificationRequest request) {
        return ResponseEntity.ok(paymentService.verifyAndConfirmPayment(request));
    }

    @Operation(summary = "Get Payment by ID", description = "Fetch a specific payment record by its UUID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<PaymentResponse> getById(
            @Parameter(description = "Payment UUID") @PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @Operation(summary = "Get Payment by Booking ID", description = "Retrieve payment status for a given booking.")
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<PaymentResponse> getByBooking(
            @Parameter(description = "Booking UUID") @PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @Operation(summary = "Get Payments by User ID", description = "List all payments made by a specific user.")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getByUser(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    @Operation(
        summary = "Refund Payment",
        description = "Initiates a full or partial refund for a PAID payment via Razorpay. Only ADMIN or the booking owner can request refunds."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund initiated successfully"),
        @ApiResponse(responseCode = "400", description = "Payment is not in PAID status"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<PaymentResponse> refund(@Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.refundPayment(request));
    }

    @Operation(
        summary = "Get Total Revenue (Admin)",
        description = "Returns the total revenue from all PAID payments. Requires ADMIN role."
    )
    @GetMapping("/admin/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Double>> getRevenue() {
        return ResponseEntity.ok(Map.of("totalRevenue", paymentService.getTotalRevenue()));
    }

    @Operation(
        summary = "Get Monthly Revenue (Admin)",
        description = "Returns PAID payment revenue grouped by month for the requested year. Requires ADMIN role."
    )
    @GetMapping("/admin/revenue/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<Integer, Double>> getMonthlyRevenue(@RequestParam(defaultValue = "2026") int year) {
        return ResponseEntity.ok(paymentService.getMonthlyRevenue(year));
    }

    private UUID extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtService.extractUserId(authHeader.substring(7));
        }
        return null;
    }
}
