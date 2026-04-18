package com.skybooker.notification.scheduler;

import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.enums.NotificationChannel;
import com.skybooker.notification.enums.NotificationType;
import com.skybooker.notification.repository.NotificationRepository;
import com.skybooker.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduler that runs every hour and sends check-in reminder notifications
 * to passengers whose flight departs within the next 24 hours.
 *
 * Flow:
 *  1. Calls booking-service internal endpoint to get confirmed bookings
 *     departing in the 23h–25h window (covering the hourly run overlap).
 *  2. For each booking, sends an in-app notification (and email if address available).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckInReminderScheduler {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

    @Value("${services.booking-url}")
    private String bookingServiceUrl;

    @Value("${app.notification.from-email:noreply@skybooker.com}")
    private String fromEmail;

    @SuppressWarnings("unchecked")
    @Scheduled(cron = "0 0 * * * *")   // every hour, on the hour
    public void sendCheckInReminders() {
        LocalDateTime windowStart = LocalDateTime.now().plusHours(23);
        LocalDateTime windowEnd   = LocalDateTime.now().plusHours(25);

        log.info("Check-in reminder scheduler running for departures between {} and {}", windowStart, windowEnd);

        try {
            // Call booking-service internal endpoint for departing bookings in window
            String url = bookingServiceUrl + "/api/v1/bookings/internal/departing"
                    + "?from=" + windowStart + "&to=" + windowEnd;

            List<Map<String, Object>> bookings = restTemplate.getForObject(url, List.class);
            if (bookings == null || bookings.isEmpty()) {
                log.info("No bookings in check-in reminder window");
                return;
            }

            log.info("{} booking(s) found for check-in reminder", bookings.size());

            for (Map<String, Object> b : bookings) {
                UUID userId    = UUID.fromString((String) b.get("userId"));
                UUID bookingId = UUID.fromString((String) b.get("bookingId"));
                String pnr     = (String) b.get("pnrCode");
                String email   = (String) b.get("contactEmail");
                String dept    = (String) b.get("departureTime");

                String title   = "Web Check-In is Now Open! ✈️";
                String message = String.format(
                        "Your flight departs at %s (PNR: %s). Online check-in is open until 1 hour before departure.",
                        dept, pnr);

                // In-app notification
                Notification notification = Notification.builder()
                        .recipientId(userId)
                        .type(NotificationType.CHECKIN_REMINDER)
                        .channel(NotificationChannel.APP)
                        .title(title)
                        .message(message)
                        .relatedBookingId(bookingId)
                        .isRead(false)
                        .build();
                notificationRepository.save(notification);

                // Email notification
                if (email != null && !email.isBlank()) {
                    try {
                        String htmlBody = buildCheckInEmail(pnr, dept);
                        emailService.sendEmail(email, title, htmlBody);
                    } catch (Exception e) {
                        log.warn("Failed to send check-in email to {}: {}", email, e.getMessage());
                    }
                }

                log.info("Check-in reminder sent for bookingId={} pnr={}", bookingId, pnr);
            }

        } catch (Exception e) {
            log.error("Check-in reminder scheduler failed: {}", e.getMessage(), e);
        }
    }

    private String buildCheckInEmail(String pnr, String departureTime) {
        return String.format("""
                <html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                <div style="background: #1a73e8; color: white; padding: 20px; border-radius: 8px 8px 0 0;">
                  <h1 style="margin:0;">✈️ SkyBooker</h1>
                  <p style="margin:4px 0;">Web Check-In is Now Open</p>
                </div>
                <div style="padding: 24px; background: #f9f9f9;">
                  <p>Your flight departs at <strong>%s</strong>.</p>
                  <p>PNR: <strong>%s</strong></p>
                  <p>Online check-in is open now and closes <strong>1 hour before departure</strong>.</p>
                  <p>Log in to SkyBooker, go to <em>My Bookings</em>, and complete your check-in to select your seat.</p>
                  <p style="color:#888; font-size:12px;">This is an automated message. Please do not reply.</p>
                </div>
                </body></html>
                """, departureTime, pnr);
    }
}
