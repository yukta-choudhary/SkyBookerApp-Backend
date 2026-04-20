package com.skybooker.flight.controller;

import com.skybooker.flight.dto.RoundTripResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Tag(name = "Flights", description = "Flight schedule management, real-time status updates, and flight search APIs")
public class FlightController {

    private final FlightService flightService;

    @Operation(summary = "Add Flight", description = "Create a new flight schedule. Requires AIRLINE_STAFF or ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flight created"),
        @ApiResponse(responseCode = "403", description = "Forbidden — staff/admin role required")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public Flight addFlight(@RequestBody Flight flight) {
        return flightService.addFlight(flight);
    }

    @Operation(summary = "Get Flight by ID", description = "Fetch a flight's details by its UUID.")
    @ApiResponse(responseCode = "404", description = "Flight not found")
    @GetMapping("/{id}")
    public Flight getFlight(@Parameter(description = "Flight UUID") @PathVariable UUID id) {
        return flightService.getFlightById(id);
    }

    @Operation(summary = "Get Flights by Airline", description = "List all flights operated by a specific airline.")
    @GetMapping("/airline/{airlineId}")
    public List<Flight> getByAirline(@PathVariable UUID airlineId) {
        return flightService.getFlightsByAirline(airlineId);
    }

    @Operation(
        summary = "Search Flights (One-Way)",
        description = "Search available flights between two airports on a specific departure date. Date format: YYYY-MM-DD."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flights found (may be empty list)"),
        @ApiResponse(responseCode = "400", description = "Invalid date format")
    })
    @GetMapping("/search")
    public List<Flight> searchFlights(
            @Parameter(description = "Origin IATA airport code (e.g. DEL)") @RequestParam String origin,
            @Parameter(description = "Destination IATA airport code (e.g. BOM)") @RequestParam String destination,
            @Parameter(description = "Departure date (YYYY-MM-DD)") @RequestParam String date
    ) {
        return flightService.searchFlights(origin, destination, LocalDate.parse(date));
    }

    @Operation(
        summary = "Search Flights (Round-Trip)",
        description = "Search outbound and return flights for a round-trip journey. Returns both legs in a single response."
    )
    @GetMapping("/search/round-trip")
    public RoundTripResponse searchRoundTrip(
            @Parameter(description = "Origin IATA code") @RequestParam String origin,
            @Parameter(description = "Destination IATA code") @RequestParam String destination,
            @Parameter(description = "Departure date (YYYY-MM-DD)") @RequestParam String departureDate,
            @Parameter(description = "Return date (YYYY-MM-DD)") @RequestParam String returnDate
    ) {
        return flightService.searchRoundTrip(
                origin, destination,
                LocalDate.parse(departureDate),
                LocalDate.parse(returnDate)
        );
    }

    @Operation(summary = "Update Flight", description = "Update flight details. Requires AIRLINE_STAFF or ADMIN role.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public Flight updateFlight(@PathVariable UUID id, @RequestBody Flight flight) {
        return flightService.updateFlight(id, flight);
    }

    @Operation(
        summary = "Update Flight Status",
        description = "Change flight status (ON_TIME, DELAYED, CANCELLED, DEPARTED, ARRIVED). Triggers a Kafka event for passenger notifications."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid status value")
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public Flight updateStatus(
            @PathVariable UUID id,
            @Parameter(description = "New status: ON_TIME | DELAYED | CANCELLED | DEPARTED | ARRIVED") @RequestParam String status) {
        return flightService.updateStatus(id, status);
    }

    @Operation(summary = "Delete Flight", description = "Permanently delete a flight by ID. Requires AIRLINE_STAFF or ADMIN role.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public void deleteFlight(@PathVariable UUID id) {
        flightService.deleteFlight(id);
    }
}