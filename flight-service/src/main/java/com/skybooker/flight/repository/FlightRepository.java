package com.skybooker.flight.repository;

import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.enums.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlightRepository extends JpaRepository<Flight, UUID> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    List<Flight> findByAirlineId(UUID airlineId);

    List<Flight> findByStatus(FlightStatus status);

    List<Flight> findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
            String origin,
            String destination,
            LocalDateTime start,
            LocalDateTime end
    );
}