package com.skybooker.airline.controller;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @PostMapping
    public ResponseEntity<Airline> create(@Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.createAirline(request));
    }

    @PutMapping("/{airlineId}")
    public ResponseEntity<Airline> update(@PathVariable String airlineId,
                                          @Valid @RequestBody AirlineRequest request) {
        return ResponseEntity.ok(airlineService.updateAirline(airlineId, request));
    }

    @GetMapping
    public ResponseEntity<List<Airline>> getAll() {
        return ResponseEntity.ok(airlineService.getAllAirlines());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Airline>> getActive() {
        return ResponseEntity.ok(airlineService.getActiveAirlines());
    }

    @GetMapping("/{airlineId}")
    public ResponseEntity<Airline> getById(@PathVariable String airlineId) {
        return ResponseEntity.ok(airlineService.getAirlineById(airlineId));
    }

    @GetMapping("/iata/{iataCode}")
    public ResponseEntity<Airline> getByIata(@PathVariable String iataCode) {
        return ResponseEntity.ok(airlineService.getAirlineByIata(iataCode));
    }

    @PatchMapping("/{airlineId}/deactivate")
    public ResponseEntity<Airline> deactivate(@PathVariable String airlineId) {
        return ResponseEntity.ok(airlineService.deactivateAirline(airlineId));
    }

    @PatchMapping("/{airlineId}/activate")
    public ResponseEntity<Airline> activate(@PathVariable String airlineId) {
        return ResponseEntity.ok(airlineService.activateAirline(airlineId));
    }
}