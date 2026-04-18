package com.skybooker.notification.repository;

import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIdOrderBySentAtDesc(UUID recipientId);

    List<Notification> findByRecipientIdAndIsReadFalseOrderBySentAtDesc(UUID recipientId);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    List<Notification> findByRelatedBookingId(UUID bookingId);

    List<Notification> findByType(NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllReadByRecipient(UUID recipientId);
}
