package com.skybooker.airline.controller;

import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.service.AirportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    @PostMapping
    public ResponseEntity<Airport> create(@Valid @RequestBody AirportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.createAirport(request));
    }

    @PutMapping("/{airportId}")
    public ResponseEntity<Airport> update(@PathVariable String airportId,
                                          @Valid @RequestBody AirportRequest request) {
        return ResponseEntity.ok(airportService.updateAirport(airportId, request));
    }

    @GetMapping
    public ResponseEntity<List<Airport>> getAll() {
        return ResponseEntity.ok(airportService.getAllAirports());
    }

    @GetMapping("/{airportId}")
    public ResponseEntity<Airport> getById(@PathVariable String airportId) {
        return ResponseEntity.ok(airportService.getAirportById(airportId));
    }

    @GetMapping("/iata/{iataCode}")
    public ResponseEntity<Airport> getByIata(@PathVariable String iataCode) {
        return ResponseEntity.ok(airportService.getAirportByIata(iataCode));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<Airport>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(airportService.getAirportsByCity(city));
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Airport>> getByCountry(@PathVariable String country) {
        return ResponseEntity.ok(airportService.getAirportsByCountry(country));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Airport>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(airportService.searchAirports(keyword));
    }

    @DeleteMapping("/{airportId}")
    public ResponseEntity<Void> delete(@PathVariable String airportId) {
        airportService.deleteAirport(airportId);
        return ResponseEntity.noContent().build();
    }
}
