package com.skybooker.notification.controller;

import com.skybooker.notification.dto.BroadcastRequest;
import com.skybooker.notification.entity.Notification;
import com.skybooker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getByRecipient(userId));
    }

    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<List<Notification>> getUnread(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getUnread(userId));
    }

    @GetMapping("/user/{userId}/unread/count")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/user/{userId}/read-all")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<Map<String, Integer>> markAllRead(@PathVariable UUID userId) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(userId)));
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID notificationId) {
        notificationService.delete(notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin broadcast: send an in-app notification to a specified list of users.
     * The frontend admin panel should first call GET /api/v1/auth/admin/users
     * to get all user IDs, then pass them in recipientIds.
     */
    @PostMapping("/admin/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody BroadcastRequest request) {
        List<Notification> sent = notificationService.sendBroadcast(request);
        return ResponseEntity.ok(Map.of(
                "sent", sent.size(),
                "title", request.getTitle()
        ));
    }
}
