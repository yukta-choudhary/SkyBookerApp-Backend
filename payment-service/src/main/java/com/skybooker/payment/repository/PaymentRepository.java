package com.skybooker.payment.repository;

import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBookingId(UUID bookingId);

    List<Payment> findByUserId(UUID userId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID'")
    Double getTotalRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID' AND p.userId = :userId")
    Double getTotalSpentByUser(@Param("userId") UUID userId);

    @Query("""
            SELECT MONTH(p.paidAt), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = 'PAID' AND YEAR(p.paidAt) = :year
            GROUP BY MONTH(p.paidAt)
            ORDER BY MONTH(p.paidAt)
            """)
    List<Object[]> getMonthlyRevenue(@Param("year") int year);
}
