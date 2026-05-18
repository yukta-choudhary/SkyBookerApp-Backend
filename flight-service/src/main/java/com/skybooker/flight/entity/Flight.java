package com.skybooker.flight.entity;

import com.skybooker.flight.enums.FlightStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue
    private UUID flightId;

    private String flightNumber;

    private UUID airlineId;

    private String originAirportCode;
    private String destinationAirportCode;

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "flight_status")
    private FlightStatus status;

    private Integer totalSeats;
    private Integer availableSeats;

    private Double basePrice;
}
