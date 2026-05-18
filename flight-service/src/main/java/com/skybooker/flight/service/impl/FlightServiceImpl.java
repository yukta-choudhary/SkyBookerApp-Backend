package com.skybooker.flight.service.impl;

import com.skybooker.flight.dto.RoundTripResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.enums.FlightStatus;
import com.skybooker.flight.event.FlightStatusChangedEvent;
import com.skybooker.flight.repository.FlightRepository;
import com.skybooker.flight.service.FlightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @CacheEvict(value = "flightSearch", allEntries = true)
    public Flight addFlight(Flight flight) {
        flight.setAvailableSeats(flight.getTotalSeats());
        flight.setDurationMinutes(calculateDurationMinutes(flight.getDepartureTime(), flight.getArrivalTime()));
        if (flight.getStatus() == null) {
            flight.setStatus(FlightStatus.ON_TIME);
        }
        return flightRepository.save(flight);
    }

    @Override
    @Transactional(readOnly = true)
    public Flight getFlightById(UUID flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found: " + flightId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flight> getFlightsByAirline(UUID airlineId) {
        return flightRepository.findByAirlineId(airlineId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "flightSearch", key = "#origin + ':' + #destination + ':' + #date")
    public List<Flight> searchFlights(String origin, String destination, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        return flightRepository
                .findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                        origin.toUpperCase(), destination.toUpperCase(), start, end);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "flightSearch", key = "'RT:' + #origin + ':' + #destination + ':' + #departureDate + ':' + #returnDate")
    public RoundTripResponse searchRoundTrip(String origin, String destination,
                                             LocalDate departureDate, LocalDate returnDate) {
        List<Flight> outbound = searchFlights(origin, destination, departureDate);
        List<Flight> returning = searchFlights(destination, origin, returnDate);
        return RoundTripResponse.builder()
                .outboundFlights(outbound)
                .returnFlights(returning)
                .build();
    }

    @Override
    @CacheEvict(value = "flightSearch", allEntries = true)
    public Flight updateFlight(UUID flightId, Flight updated) {
        Flight flight = getFlightById(flightId);
        flight.setFlightNumber(updated.getFlightNumber());
        flight.setAirlineId(updated.getAirlineId());
        flight.setOriginAirportCode(updated.getOriginAirportCode());
        flight.setDestinationAirportCode(updated.getDestinationAirportCode());
        flight.setDepartureTime(updated.getDepartureTime());
        flight.setArrivalTime(updated.getArrivalTime());
        flight.setDurationMinutes(calculateDurationMinutes(updated.getDepartureTime(), updated.getArrivalTime()));
        flight.setTotalSeats(updated.getTotalSeats());
        flight.setAvailableSeats(updated.getAvailableSeats());
        flight.setBasePrice(updated.getBasePrice());
        return flightRepository.save(flight);
    }

    @Override
    @CacheEvict(value = "flightSearch", allEntries = true)
    public Flight updateStatus(UUID flightId, String status) {
        Flight flight = getFlightById(flightId);
        FlightStatus newStatus = FlightStatus.valueOf(status.toUpperCase());
        flight.setStatus(newStatus);
        flight = flightRepository.save(flight);

        // Publish Kafka event so notification-service alerts booked passengers
        try {
            FlightStatusChangedEvent event = FlightStatusChangedEvent.builder()
                    .flightId(flightId)
                    .flightNumber(flight.getFlightNumber())
                    .newStatus(newStatus.name())
                    .origin(flight.getOriginAirportCode())
                    .destination(flight.getDestinationAirportCode())
                    .scheduledDeparture(flight.getDepartureTime() != null
                            ? flight.getDepartureTime().toString() : null)
                    .build();
            kafkaTemplate.send("flight-status-changed", flightId.toString(), event);
            log.info("Published flight-status-changed: flight={} status={}",
                    flight.getFlightNumber(), newStatus);
        } catch (Exception e) {
            log.error("Failed to publish flight-status-changed event: {}", e.getMessage());
            // Don't fail the status update — Kafka publish is best-effort
        }

        return flight;
    }

    @Override
    @CacheEvict(value = "flightSearch", allEntries = true)
    public void deleteFlight(UUID flightId) {
        flightRepository.deleteById(flightId);
    }

    private Integer calculateDurationMinutes(LocalDateTime departureTime, LocalDateTime arrivalTime) {
        if (departureTime == null || arrivalTime == null) {
            return null;
        }
        long minutes = Duration.between(departureTime, arrivalTime).toMinutes();
        if (minutes <= 0) {
            throw new RuntimeException("Arrival time must be after departure time");
        }
        return Math.toIntExact(minutes);
    }
}
