package com.skybooker.payment;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.skybooker.payment.dto.InitiatePaymentRequest;
import com.skybooker.payment.dto.PaymentResponse;
import com.skybooker.payment.dto.RefundRequest;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.enums.PaymentStatus;
import com.skybooker.payment.repository.PaymentRepository;
import com.skybooker.payment.service.impl.PaymentServiceImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RazorpayClient razorpayClient;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private PaymentServiceImpl paymentService;

    private UUID bookingId, userId, paymentId;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "rzp_test_secret");
        ReflectionTestUtils.setField(paymentService, "defaultCurrency", "INR");
        ReflectionTestUtils.setField(paymentService, "bookingServiceUrl", "http://localhost:8083");

        testPayment = Payment.builder()
                .paymentId(paymentId).bookingId(bookingId).userId(userId)
                .amount(5000.0).currency("INR").status(PaymentStatus.PENDING)
                .razorpayOrderId("order_test123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test @DisplayName("Should get payment by ID")
    void getPaymentById() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
        PaymentResponse res = paymentService.getPaymentById(paymentId);
        assertThat(res.getPaymentId()).isEqualTo(paymentId);
        assertThat(res.getAmount()).isEqualTo(5000.0);
    }

    @Test @DisplayName("Should throw when payment not found by ID")
    void getPaymentById_notFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should get payment by booking ID")
    void getPaymentByBookingId() {
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(testPayment));
        PaymentResponse res = paymentService.getPaymentByBookingId(bookingId);
        assertThat(res.getBookingId()).isEqualTo(bookingId);
    }

    @Test @DisplayName("Should throw when payment not found by booking ID")
    void getPaymentByBookingId_notFound() {
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getPaymentByBookingId(bookingId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should get payments by user")
    void getPaymentsByUser() {
        when(paymentRepository.findByUserId(userId)).thenReturn(List.of(testPayment));
        List<PaymentResponse> res = paymentService.getPaymentsByUser(userId);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getUserId()).isEqualTo(userId);
    }

    @Test @DisplayName("Should throw when refunding non-PAID payment")
    void refund_notPaid() {
        testPayment.setStatus(PaymentStatus.PENDING);
        RefundRequest req = new RefundRequest();
        req.setPaymentId(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));
        assertThatThrownBy(() -> paymentService.refundPayment(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only paid payments");
    }

    @Test @DisplayName("Should throw when initiating payment for already paid booking")
    void initiate_alreadyPaid() {
        testPayment.setStatus(PaymentStatus.PAID);
        InitiatePaymentRequest req = new InitiatePaymentRequest();
        req.setBookingId(bookingId);
        req.setAmount(5000.0);
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(testPayment));
        assertThatThrownBy(() -> paymentService.initiatePayment(req, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already paid");
    }

    @Test @DisplayName("Should get total revenue")
    void getTotalRevenue() {
        when(paymentRepository.getTotalRevenue()).thenReturn(150000.0);
        assertThat(paymentService.getTotalRevenue()).isEqualTo(150000.0);
    }

    @Test @DisplayName("Should get monthly revenue")
    void getMonthlyRevenue() {
        List<Object[]> rows = List.of(
                new Object[]{1, 10000.0},
                new Object[]{3, 25000.0}
        );
        when(paymentRepository.getMonthlyRevenue(2026)).thenReturn(rows);
        Map<Integer, Double> res = paymentService.getMonthlyRevenue(2026);
        assertThat(res).hasSize(12);
        assertThat(res.get(1)).isEqualTo(10000.0);
        assertThat(res.get(3)).isEqualTo(25000.0);
        assertThat(res.get(2)).isEqualTo(0.0); // unfilled month
    }
}
