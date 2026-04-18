package com.skybooker.flight.service.impl;

import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.enums.FlightStatus;
import com.skybooker.flight.repository.FlightRepository;
import com.skybooker.flight.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;

    @Override
    public Flight addFlight(Flight flight) {
        flight.setAvailableSeats(flight.getTotalSeats());
        if (flight.getStatus() == null) {
            flight.setStatus(FlightStatus.ON_TIME);
        }
        return flightRepository.save(flight);
    }

    @Override
    public Flight getFlightById(UUID flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
    }

    @Override
    public List<Flight> getFlightsByAirline(UUID airlineId) {
        return flightRepository.findByAirlineId(airlineId);
    }

    @Override
    public List<Flight> searchFlights(String origin, String destination, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        return flightRepository
                .findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                        origin,
                        destination,
                        start,
                        end
                );
    }

    @Override
    public Flight updateFlight(UUID flightId, Flight updated) {
        Flight flight = getFlightById(flightId);

        flight.setFlightNumber(updated.getFlightNumber());
        flight.setAirlineId(updated.getAirlineId());
        flight.setOriginAirportCode(updated.getOriginAirportCode());
        flight.setDestinationAirportCode(updated.getDestinationAirportCode());
        flight.setDepartureTime(updated.getDepartureTime());
        flight.setArrivalTime(updated.getArrivalTime());
        flight.setDurationMinutes(updated.getDurationMinutes());
        flight.setAircraftType(updated.getAircraftType());
        flight.setTotalSeats(updated.getTotalSeats());
        flight.setAvailableSeats(updated.getAvailableSeats());
        flight.setBasePrice(updated.getBasePrice());

        return flightRepository.save(flight);
    }

    @Override
    public Flight updateStatus(UUID flightId, String status) {
        Flight flight = getFlightById(flightId);
        flight.setStatus(FlightStatus.valueOf(status.toUpperCase()));
        return flightRepository.save(flight);
    }

    @Override
    public void deleteFlight(UUID flightId) {
        flightRepository.deleteById(flightId);
    }
}