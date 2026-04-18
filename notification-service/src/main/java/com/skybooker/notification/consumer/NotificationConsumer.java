package com.skybooker.notification.consumer;

import com.skybooker.notification.event.FlightStatusChangedEvent;
import com.skybooker.notification.event.PaymentSuccessEvent;
import com.skybooker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment-success",
            groupId = "notification-group",
            containerFactory = "paymentSuccessKafkaListenerContainerFactory"
    )
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received payment-success event for bookingId={}", event.getBookingId());
        try {
            notificationService.handlePaymentSuccess(event);
        } catch (Exception e) {
            log.error("Error processing payment-success event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "flight-status-changed",
            groupId = "notification-group",
            containerFactory = "flightStatusKafkaListenerContainerFactory"
    )
    public void handleFlightStatusChanged(FlightStatusChangedEvent event) {
        log.info("Received flight-status-changed event for flight={}", event.getFlightNumber());
        try {
            // In a real system, fetch affected userIds from booking-service
            // For now, log and handle with empty list (extend later)
            notificationService.handleFlightStatusChanged(event, List.of());
        } catch (Exception e) {
            log.error("Error processing flight-status-changed event: {}", e.getMessage(), e);
        }
    }
}
