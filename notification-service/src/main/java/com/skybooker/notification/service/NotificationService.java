package com.skybooker.notification.service;

import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.enums.NotificationChannel;
import com.skybooker.notification.enums.NotificationType;
import com.skybooker.notification.event.PaymentSuccessEvent;
import com.skybooker.notification.event.FlightStatusChangedEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    Notification save(UUID recipientId, NotificationType type, NotificationChannel channel,
                      String title, String message, UUID relatedBookingId);

    void handlePaymentSuccess(PaymentSuccessEvent event);

    void handleFlightStatusChanged(FlightStatusChangedEvent event, List<UUID> affectedUserIds);

    List<Notification> getByRecipient(UUID recipientId);

    List<Notification> getUnread(UUID recipientId);

    long getUnreadCount(UUID recipientId);

    Notification markAsRead(UUID notificationId);

    int markAllRead(UUID recipientId);

    void delete(UUID notificationId);
}
