package com.skybooker.flight.service;

import com.skybooker.flight.entity.Flight;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FlightService {

    Flight addFlight(Flight flight);

    Flight getFlightById(UUID flightId);

    List<Flight> getFlightsByAirline(UUID airlineId);

    List<Flight> searchFlights(String origin, String destination, LocalDate date);

    Flight updateFlight(UUID flightId, Flight flight);

    Flight updateStatus(UUID flightId, String status);

    void deleteFlight(UUID flightId);
}