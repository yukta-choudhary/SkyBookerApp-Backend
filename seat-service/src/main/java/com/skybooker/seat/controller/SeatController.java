package com.skybooker.seat.controller;

import com.skybooker.seat.dto.AddSeatsForFlightRequest;
import com.skybooker.seat.dto.HoldSeatRequest;
import com.skybooker.seat.dto.UpdateSeatRequest;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.enums.SeatClass;
import com.skybooker.seat.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public List<Seat> addSeats(@RequestBody @Valid AddSeatsForFlightRequest request) {
        return seatService.addSeatsForFlight(request);
    }

    @GetMapping("/flight/{flightId}/available")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public List<Seat> getAvailableSeats(@PathVariable UUID flightId) {
        return seatService.getAvailableSeats(flightId);
    }

    @GetMapping("/flight/{flightId}/class/{seatClass}")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public List<Seat> getAvailableByClass(@PathVariable UUID flightId,
                                          @PathVariable SeatClass seatClass) {
        return seatService.getAvailableByClass(flightId, seatClass);
    }

    @GetMapping("/{seatId}")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public Seat getSeatById(@PathVariable UUID seatId) {
        return seatService.getSeatById(seatId);
    }

    @PutMapping("/hold")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public Seat holdSeat(@RequestBody @Valid HoldSeatRequest request) {
        return seatService.holdSeat(request.getSeatId());
    }

    @PutMapping("/{seatId}/release")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public Seat releaseSeat(@PathVariable UUID seatId) {
        return seatService.releaseSeat(seatId);
    }

    @PutMapping("/{seatId}/confirm")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public Seat confirmSeat(@PathVariable UUID seatId) {
        return seatService.confirmSeat(seatId);
    }

    @PutMapping("/{seatId}")
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public Seat updateSeat(@PathVariable UUID seatId,
                           @RequestBody @Valid UpdateSeatRequest request) {
        return seatService.updateSeat(seatId, request);
    }

    @GetMapping("/flight/{flightId}/map")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public List<Seat> getSeatMap(@PathVariable UUID flightId) {
        return seatService.getSeatMap(flightId);
    }

    @GetMapping("/flight/{flightId}/count")
    @PreAuthorize("hasAnyRole('PASSENGER','AIRLINE_STAFF','ADMIN')")
    public long countAvailableByClass(@PathVariable UUID flightId,
                                      @RequestParam SeatClass seatClass) {
        return seatService.countAvailableByClass(flightId, seatClass);
    }

    @DeleteMapping("/flight/{flightId}")
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public void deleteSeatsForFlight(@PathVariable UUID flightId) {
        seatService.deleteSeatsForFlight(flightId);
    }
}