package com.skybooker.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightStatusChangedEvent {
    private UUID flightId;
    private String flightNumber;
    private String newStatus;      // DELAYED, CANCELLED, ON_TIME
    private String origin;
    private String destination;
    private String departureTime;
    private String delayReason;
    private String newGate;
}
