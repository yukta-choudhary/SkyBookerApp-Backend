package com.skybooker.payment;

import com.razorpay.RazorpayClient;
import com.skybooker.payment.dto.InitiatePaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.dto.PaymentVerificationRequest;
import com.skybooker.payment.dto.RefundRequest;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.enums.PaymentStatus;
import com.skybooker.payment.repository.PaymentRepository;
import com.skybooker.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.
 * Covers initiate payment, verify payment, get payment, refund, and revenue.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RazorpayClient razorpayClient;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID bookingId;
    private UUID userId;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .bookingId(bookingId)
                .userId(userId)
                .amount(5000.0)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .razorpayOrderId("order_testId123")
                .createdAt(LocalDateTime.now())
                .build();

        // Inject required @Value fields via ReflectionTestUtils
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret_key_32chars_long_x1234");
        ReflectionTestUtils.setField(paymentService, "defaultCurrency", "INR");
        ReflectionTestUtils.setField(paymentService, "bookingServiceUrl", "http://localhost:8084");
    }

    // ===== INITIATE PAYMENT TESTS =====

    @Test
    @DisplayName("InitiatePayment: should throw when Razorpay fails")
    void initiatePayment_shouldThrow_whenRazorpayFails() throws Exception {
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(bookingId);
        request.setAmount(5000.0);
        request.setCurrency("INR");

        // Mock no existing payment
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        // razorpayClient.orders is null by default in mocked object → triggers RazorpayException or NPE
        // which gets caught and wrapped as RuntimeException
        assertThatThrownBy(() -> paymentService.initiatePayment(request, userId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("InitiatePayment: should throw if booking already paid")
    void initiatePayment_shouldThrow_whenAlreadyPaid() {
        testPayment.setStatus(PaymentStatus.PAID);
        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setBookingId(bookingId);
        request.setAmount(5000.0);

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() -> paymentService.initiatePayment(request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already paid");
    }

    // ===== GET PAYMENT TESTS =====

    @Test
    @DisplayName("GetPaymentById: should return payment for valid ID")
    void getPaymentById_shouldReturnPayment() {
        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.of(testPayment));

        PaymentResponse response = paymentService.getPaymentById(testPayment.getPaymentId());

        assertThat(response).isNotNull();
        assertThat(response.getBookingId()).isEqualTo(bookingId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("GetPaymentById: should throw for unknown ID")
    void getPaymentById_shouldThrow_whenNotFound() {
        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("GetPaymentByBookingId: should return payment for valid booking")
    void getPaymentByBookingId_shouldReturnPayment() {
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(testPayment));

        PaymentResponse response = paymentService.getPaymentByBookingId(bookingId);

        assertThat(response.getBookingId()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("GetPaymentsByUser: should return list of payments for user")
    void getPaymentsByUser_shouldReturnList() {
        when(paymentRepository.findByUserId(userId)).thenReturn(List.of(testPayment));

        List<PaymentResponse> payments = paymentService.getPaymentsByUser(userId);

        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("GetPaymentsByUser: should return empty list when no payments")
    void getPaymentsByUser_shouldReturnEmpty_whenNoneExist() {
        when(paymentRepository.findByUserId(userId)).thenReturn(List.of());

        List<PaymentResponse> payments = paymentService.getPaymentsByUser(userId);

        assertThat(payments).isEmpty();
    }

    // ===== VERIFY PAYMENT TESTS =====

    @Test
    @DisplayName("VerifyPayment: should mark payment paid and confirm booking")
    void verifyPayment_shouldMarkPaidAndConfirmBooking() {
        PaymentVerificationRequest request = verificationRequest();
        when(paymentRepository.findByRazorpayOrderId(testPayment.getRazorpayOrderId()))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.verifyAndConfirmPayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.getRazorpayPaymentId()).isEqualTo(request.getRazorpayPaymentId());
        assertThat(response.getTransactionId()).isEqualTo(request.getRazorpayPaymentId());
        assertThat(response.getPaidAt()).isNotNull();
        verify(restTemplate).put("http://localhost:8084/api/v1/bookings/internal/" + bookingId + "/confirm", null);
    }

    @Test
    @DisplayName("VerifyPayment: should fail after retries when booking cannot be confirmed")
    void verifyPayment_shouldThrow_whenBookingConfirmationFails() {
        PaymentVerificationRequest request = verificationRequest();
        when(paymentRepository.findByRazorpayOrderId(testPayment.getRazorpayOrderId()))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RestClientException("booking-service unavailable"))
                .when(restTemplate).put(anyString(), isNull());

        assertThatThrownBy(() -> paymentService.verifyAndConfirmPayment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("booking confirmation failed");

        verify(restTemplate, times(3)).put("http://localhost:8084/api/v1/bookings/internal/" + bookingId + "/confirm", null);
    }

    // ===== REFUND TESTS =====

    @Test
    @DisplayName("Refund: should throw if payment is not PAID")
    void refund_shouldThrow_whenNotPaid() {
        testPayment.setStatus(PaymentStatus.PENDING); // not paid
        RefundRequest req = new RefundRequest();
        req.setPaymentId(testPayment.getPaymentId());

        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() -> paymentService.refundPayment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only paid payments");
    }

    @Test
    @DisplayName("Refund: should throw if payment not found")
    void refund_shouldThrow_whenPaymentNotFound() {
        RefundRequest req = new RefundRequest();
        req.setPaymentId(UUID.randomUUID());

        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");
    }

    // ===== GET TOTAL REVENUE TEST =====

    @Test
    @DisplayName("GetTotalRevenue: should return total revenue from repository")
    void getTotalRevenue_shouldReturnValue() {
        when(paymentRepository.getTotalRevenue()).thenReturn(150000.0);

        Double revenue = paymentService.getTotalRevenue();

        assertThat(revenue).isEqualTo(150000.0);
    }

    @Test
    @DisplayName("GetTotalRevenue: should return 0 when no payments")
    void getTotalRevenue_shouldReturnZero_whenNoPayments() {
        when(paymentRepository.getTotalRevenue()).thenReturn(0.0);

        Double revenue = paymentService.getTotalRevenue();

        assertThat(revenue).isZero();
    }

    @Test
    @DisplayName("GetMonthlyRevenue: should return all months with repository values filled in")
    void getMonthlyRevenue_shouldReturnAllMonths() {
        when(paymentRepository.getMonthlyRevenue(2026)).thenReturn(List.of(
                new Object[] { 5, 25000.0 },
                new Object[] { 6, 17500.0 }
        ));

        Map<Integer, Double> revenue = paymentService.getMonthlyRevenue(2026);

        assertThat(revenue).hasSize(12);
        assertThat(revenue.get(1)).isZero();
        assertThat(revenue.get(5)).isEqualTo(25000.0);
        assertThat(revenue.get(6)).isEqualTo(17500.0);
    }

    private PaymentVerificationRequest verificationRequest() {
        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setBookingId(bookingId);
        request.setRazorpayOrderId(testPayment.getRazorpayOrderId());
        request.setRazorpayPaymentId("pay_testId123");
        request.setRazorpaySignature(signatureFor(request.getRazorpayOrderId(), request.getRazorpayPaymentId()));
        return request;
    }

    private String signatureFor(String orderId, String paymentId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("test_secret_key_32chars_long_x1234".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
