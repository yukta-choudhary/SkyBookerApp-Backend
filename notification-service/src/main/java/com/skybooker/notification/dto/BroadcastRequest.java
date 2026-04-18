package com.skybooker.notification.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BroadcastRequest {

    private String title;
    private String message;

    /**
     * Optional: specific recipient IDs to target.
     * If provided, the broadcast is sent only to these users.
     * If null/empty, use targetRole to determine recipients.
     */
    private List<UUID> recipientIds;

    /**
     * Optional: "PASSENGER" | "AIRLINE_STAFF" | "ADMIN" | "ALL"
     * Used when recipientIds is not provided (admin-level broadcast).
     */
    private String targetRole;
}
