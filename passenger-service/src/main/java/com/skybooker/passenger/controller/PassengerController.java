package com.skybooker.passenger.controller;

import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    public PassengerInfo addPassenger(@RequestBody PassengerInfo passenger) {
        return passengerService.addPassenger(passenger);
    }

    @GetMapping("/{id}")
    public PassengerInfo getById(@PathVariable UUID id) {
        return passengerService.getPassengerById(id);
    }

    @GetMapping("/booking/{bookingId}")
    public List<PassengerInfo> getByBooking(@PathVariable UUID bookingId) {
        return passengerService.getPassengersByBooking(bookingId);
    }

    @PutMapping("/{id}")
    public PassengerInfo update(@PathVariable UUID id, @RequestBody PassengerInfo passenger) {
        return passengerService.updatePassenger(id, passenger);
    }

    @PutMapping("/{id}/assign-seat")
    public PassengerInfo assignSeat(
            @PathVariable UUID id,
            @RequestParam UUID seatId,
            @RequestParam String seatNumber
    ) {
        return passengerService.assignSeat(id, seatId, seatNumber);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        passengerService.deletePassenger(id);
    }

    @GetMapping("/count/{bookingId}")
    public long count(@PathVariable UUID bookingId) {
        return passengerService.getPassengerCount(bookingId);
    }
}