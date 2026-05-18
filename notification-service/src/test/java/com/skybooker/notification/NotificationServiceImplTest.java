package com.skybooker.notification;

import com.skybooker.notification.dto.BroadcastRequest;
import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.enums.NotificationChannel;
import com.skybooker.notification.enums.NotificationType;
import com.skybooker.notification.event.FlightStatusChangedEvent;
import com.skybooker.notification.event.PaymentSuccessEvent;
import com.skybooker.notification.repository.NotificationRepository;
import com.skybooker.notification.service.EmailService;
import com.skybooker.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;
    @InjectMocks private NotificationServiceImpl notificationService;

    private UUID recipientId, bookingId, notificationId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        testNotification = Notification.builder()
                .notificationId(notificationId).recipientId(recipientId)
                .type(NotificationType.GENERAL).channel(NotificationChannel.APP)
                .title("Test").message("Test message").isRead(false).build();
    }

    @Test @DisplayName("Should save notification")
    void save() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Notification res = notificationService.save(recipientId, NotificationType.GENERAL,
                NotificationChannel.APP, "Title", "Msg", bookingId);
        assertThat(res.getRecipientId()).isEqualTo(recipientId);
        assertThat(res.getTitle()).isEqualTo("Title");
        assertThat(res.getIsRead()).isFalse();
    }

    @Test @DisplayName("Should handle payment success event with email")
    void handlePaymentSuccess_withEmail() {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setUserId(recipientId);
        event.setBookingId(bookingId);
        event.setPnrCode("ABC123");
        event.setAmount(5000.0);
        event.setUserEmail("user@test.com");
        event.setUserFullName("John Doe");
        event.setCurrency("INR");

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.handlePaymentSuccess(event);

        verify(notificationRepository, atLeast(1)).save(any());
        verify(emailService).sendEmail(eq("user@test.com"), anyString(), anyString());
    }

    @Test @DisplayName("Should handle payment success without email")
    void handlePaymentSuccess_noEmail() {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setUserId(recipientId);
        event.setBookingId(bookingId);
        event.setAmount(3000.0);

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.handlePaymentSuccess(event);

        verify(notificationRepository, times(1)).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test @DisplayName("Should handle payment success when email fails gracefully")
    void handlePaymentSuccess_emailFails() {
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setUserId(recipientId);
        event.setBookingId(bookingId);
        event.setAmount(5000.0);
        event.setUserEmail("user@test.com");
        event.setUserFullName("John");

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("SMTP down")).when(emailService).sendEmail(anyString(), anyString(), anyString());

        // Should not throw
        assertThatCode(() -> notificationService.handlePaymentSuccess(event)).doesNotThrowAnyException();
    }

    @Test @DisplayName("Should handle DELAYED flight status change")
    void handleFlightStatus_delayed() {
        FlightStatusChangedEvent event = new FlightStatusChangedEvent();
        event.setFlightNumber("SK101");
        event.setNewStatus("DELAYED");
        event.setOrigin("DEL");
        event.setDestination("BOM");

        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.handleFlightStatusChanged(event, List.of(user1, user2));

        verify(notificationRepository, times(2)).save(any());
    }

    @Test @DisplayName("Should handle CANCELLED flight status change")
    void handleFlightStatus_cancelled() {
        FlightStatusChangedEvent event = new FlightStatusChangedEvent();
        event.setFlightNumber("SK102");
        event.setNewStatus("CANCELLED");
        event.setOrigin("DEL");
        event.setDestination("BLR");

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        notificationService.handleFlightStatusChanged(event, List.of(recipientId));
        verify(notificationRepository).save(argThat(n -> n.getType() == NotificationType.FLIGHT_CANCELLATION));
    }

    @Test @DisplayName("Should get notifications by recipient")
    void getByRecipient() {
        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(recipientId))
                .thenReturn(List.of(testNotification));
        assertThat(notificationService.getByRecipient(recipientId)).hasSize(1);
    }

    @Test @DisplayName("Should get unread notifications")
    void getUnread() {
        when(notificationRepository.findByRecipientIdAndIsReadFalseOrderBySentAtDesc(recipientId))
                .thenReturn(List.of(testNotification));
        assertThat(notificationService.getUnread(recipientId)).hasSize(1);
    }

    @Test @DisplayName("Should get unread count")
    void getUnreadCount() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(recipientId)).thenReturn(5L);
        assertThat(notificationService.getUnreadCount(recipientId)).isEqualTo(5);
    }

    @Test @DisplayName("Should mark notification as read")
    void markAsRead() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Notification res = notificationService.markAsRead(notificationId);
        assertThat(res.getIsRead()).isTrue();
    }

    @Test @DisplayName("Should throw when marking non-existent notification")
    void markAsRead_notFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should mark all read for recipient")
    void markAllRead() {
        when(notificationRepository.markAllReadByRecipient(recipientId)).thenReturn(5);
        assertThat(notificationService.markAllRead(recipientId)).isEqualTo(5);
    }

    @Test @DisplayName("Should delete notification")
    void delete() {
        notificationService.delete(notificationId);
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test @DisplayName("Should send broadcast to specific recipients")
    void sendBroadcast_withRecipients() {
        UUID r1 = UUID.randomUUID();
        UUID r2 = UUID.randomUUID();
        BroadcastRequest req = new BroadcastRequest();
        req.setTitle("Announcement");
        req.setMessage("System maintenance tonight");
        req.setRecipientIds(List.of(r1, r2));

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Notification> res = notificationService.sendBroadcast(req);

        assertThat(res).hasSize(2);
        verify(notificationRepository, times(2)).save(any());
    }

    @Test @DisplayName("Should return empty list for broadcast without recipients")
    void sendBroadcast_noRecipients() {
        BroadcastRequest req = new BroadcastRequest();
        req.setTitle("Announcement");
        req.setMessage("Maintenance");

        List<Notification> res = notificationService.sendBroadcast(req);

        assertThat(res).isEmpty();
    }

    @Test @DisplayName("Should throw for broadcast without title")
    void sendBroadcast_noTitle() {
        BroadcastRequest req = new BroadcastRequest();
        req.setMessage("msg");

        assertThatThrownBy(() -> notificationService.sendBroadcast(req))
                .isInstanceOf(RuntimeException.class);
    }
}
