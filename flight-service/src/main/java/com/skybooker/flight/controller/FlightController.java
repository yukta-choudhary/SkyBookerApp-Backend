package com.skybooker.flight.controller;

import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    public Flight addFlight(@RequestBody Flight flight) {
        return flightService.addFlight(flight);
    }

    @GetMapping("/{id}")
    public Flight getFlight(@PathVariable UUID id) {
        return flightService.getFlightById(id);
    }

    @GetMapping("/airline/{airlineId}")
    public List<Flight> getByAirline(@PathVariable UUID airlineId) {
        return flightService.getFlightsByAirline(airlineId);
    }

    @GetMapping("/search")
    public List<Flight> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String date
    ) {
        return flightService.searchFlights(
                origin,
                destination,
                LocalDate.parse(date)
        );
    }

    @PutMapping("/{id}")
    public Flight updateFlight(@PathVariable UUID id, @RequestBody Flight flight) {
        return flightService.updateFlight(id, flight);
    }

    @PutMapping("/{id}/status")
    public Flight updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return flightService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public void deleteFlight(@PathVariable UUID id) {
        flightService.deleteFlight(id);
    }
}