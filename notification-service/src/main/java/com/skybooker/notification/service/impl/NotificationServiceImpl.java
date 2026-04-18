package com.skybooker.notification.service.impl;

import com.skybooker.notification.dto.BroadcastRequest;
import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.enums.NotificationChannel;
import com.skybooker.notification.enums.NotificationType;
import com.skybooker.notification.event.FlightStatusChangedEvent;
import com.skybooker.notification.event.PaymentSuccessEvent;
import com.skybooker.notification.repository.NotificationRepository;
import com.skybooker.notification.service.EmailService;
import com.skybooker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Override
    public Notification save(UUID recipientId, NotificationType type, NotificationChannel channel,
                             String title, String message, UUID relatedBookingId) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .channel(channel)
                .title(title)
                .message(message)
                .relatedBookingId(relatedBookingId)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Override
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        String title = "Booking Confirmed! ✈️";
        String pnr = event.getPnrCode() != null ? event.getPnrCode() : "N/A";
        String message = String.format(
                "Your booking (PNR: %s) is confirmed. Amount paid: ₹%.2f. Have a great flight!",
                pnr, event.getAmount()
        );

        // 1. Save in-app notification
        save(event.getUserId(), NotificationType.BOOKING_CONFIRMED, NotificationChannel.APP,
                title, message, event.getBookingId());

        // 2. Send email
        if (event.getUserEmail() != null) {
            String emailBody = buildBookingConfirmationEmail(event);
            try {
                emailService.sendEmail(event.getUserEmail(), title, emailBody);
                save(event.getUserId(), NotificationType.PAYMENT_SUCCESS, NotificationChannel.EMAIL,
                        title, "Confirmation email sent to " + event.getUserEmail(), event.getBookingId());
            } catch (Exception e) {
                log.error("Failed to send confirmation email to {}: {}", event.getUserEmail(), e.getMessage());
            }
        }

        log.info("Booking confirmed notifications sent for bookingId={}", event.getBookingId());
    }

    @Override
    public void handleFlightStatusChanged(FlightStatusChangedEvent event, List<UUID> affectedUserIds) {
        NotificationType type = switch (event.getNewStatus()) {
            case "DELAYED" -> NotificationType.FLIGHT_DELAY;
            case "CANCELLED" -> NotificationType.FLIGHT_CANCELLATION;
            case "GATE_CHANGE" -> NotificationType.GATE_CHANGE;
            default -> NotificationType.GENERAL;
        };

        String title = switch (event.getNewStatus()) {
            case "DELAYED" -> "Flight Delayed ⏰";
            case "CANCELLED" -> "Flight Cancelled ❌";
            case "GATE_CHANGE" -> "Gate Changed 🚪";
            default -> "Flight Update";
        };

        String message = String.format("Flight %s (%s → %s): %s. %s",
                event.getFlightNumber(), event.getOrigin(), event.getDestination(),
                event.getNewStatus(),
                event.getDelayReason() != null ? event.getDelayReason() : "");

        for (UUID userId : affectedUserIds) {
            save(userId, type, NotificationChannel.APP, title, message, null);
        }

        log.info("Flight status notifications sent to {} passengers for flight {}",
                affectedUserIds.size(), event.getFlightNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getByRecipient(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnread(UUID recipientId) {
        return notificationRepository.findByRecipientIdAndIsReadFalseOrderBySentAtDesc(recipientId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Override
    public Notification markAsRead(UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setIsRead(true);
        return notificationRepository.save(n);
    }

    @Override
    public int markAllRead(UUID recipientId) {
        return notificationRepository.markAllReadByRecipient(recipientId);
    }

    @Override
    public void delete(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public List<Notification> sendBroadcast(BroadcastRequest request) {
        if (request.getTitle() == null || request.getMessage() == null) {
            throw new RuntimeException("Broadcast title and message are required");
        }

        List<UUID> recipients = request.getRecipientIds();

        if (recipients == null || recipients.isEmpty()) {
            // No specific recipients — broadcast to all stored notification recipients
            // (In production, call auth-service to get users by role)
            // For now: fetch all distinct recipientIds from existing notifications as a fallback
            log.warn("sendBroadcast called without recipientIds — " +
                    "targetRole='{}' broadcast requires caller to pass recipientIds from auth-service",
                    request.getTargetRole());
            return List.of();
        }

        List<Notification> saved = recipients.stream()
                .map(uid -> save(uid, NotificationType.GENERAL, NotificationChannel.APP,
                        request.getTitle(), request.getMessage(), null))
                .toList();

        log.info("Admin broadcast sent to {} recipient(s)", saved.size());
        return saved;
    }

    // ── Email body builders ─────────────────────────────────────────────────

    private String buildBookingConfirmationEmail(PaymentSuccessEvent event) {
        String name = event.getUserFullName() != null ? event.getUserFullName() : "Passenger";
        String pnr = event.getPnrCode() != null ? event.getPnrCode() : "N/A";
        String flight = event.getFlightNumber() != null ? event.getFlightNumber() : "N/A";
        String route = (event.getOrigin() != null && event.getDestination() != null)
                ? event.getOrigin() + " → " + event.getDestination() : "N/A";

        return String.format("""
                <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                <div style="background: #1a73e8; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                  <h1 style="margin:0;">✈️ SkyBooker</h1>
                  <p style="margin:5px 0;">Booking Confirmed!</p>
                </div>
                <div style="padding: 24px; background: #f9f9f9;">
                  <p>Dear <strong>%s</strong>,</p>
                  <p>Your booking has been <strong>confirmed</strong>. Here are your details:</p>
                  <table style="width:100%%; border-collapse: collapse; margin: 16px 0;">
                    <tr style="background:#e8f0fe;"><td style="padding:10px;"><strong>PNR Code</strong></td><td style="padding:10px;"><strong>%s</strong></td></tr>
                    <tr><td style="padding:10px;">Flight</td><td style="padding:10px;">%s</td></tr>
                    <tr style="background:#e8f0fe;"><td style="padding:10px;">Route</td><td style="padding:10px;">%s</td></tr>
                    <tr><td style="padding:10px;">Amount Paid</td><td style="padding:10px;">₹%.2f %s</td></tr>
                    <tr style="background:#e8f0fe;"><td style="padding:10px;">Transaction ID</td><td style="padding:10px;">%s</td></tr>
                  </table>
                  <p>Please check in online 24 hours before departure.</p>
                  <p style="color:#666; font-size:12px;">This is an automated email. Please do not reply.</p>
                </div>
                </body></html>
                """,
                name, pnr, flight, route,
                event.getAmount() != null ? event.getAmount() : 0.0,
                event.getCurrency() != null ? event.getCurrency() : "INR",
                event.getRazorpayPaymentId() != null ? event.getRazorpayPaymentId() : "N/A"
        );
    }
}
