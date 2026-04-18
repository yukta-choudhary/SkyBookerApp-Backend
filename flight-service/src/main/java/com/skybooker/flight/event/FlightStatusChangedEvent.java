package com.skybooker.flight.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightStatusChangedEvent {
    private UUID flightId;
    private String flightNumber;
    private String newStatus;       // DELAYED, CANCELLED, ON_TIME, DEPARTED, ARRIVED
    private String origin;
    private String destination;
    private String scheduledDeparture;
    private String delayReason;
    private String newGate;
}
