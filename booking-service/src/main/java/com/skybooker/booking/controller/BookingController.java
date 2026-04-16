package com.skybooker.booking.controller;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public Booking create(@RequestBody Booking booking) {
        return bookingService.createBooking(booking);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public Booking getById(@PathVariable UUID id) {
        return bookingService.getBookingById(id);
    }

    @GetMapping("/pnr/{pnr}")
    public Booking getByPnr(@PathVariable String pnr) {
        return bookingService.getBookingByPnr(pnr);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public List<Booking> getByUser(@PathVariable UUID userId) {
        return bookingService.getBookingsByUser(userId);
    }

    @GetMapping("/flight/{flightId}")
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public List<Booking> getByFlight(@PathVariable UUID flightId) {
        return bookingService.getBookingsByFlight(flightId);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public Booking cancel(@PathVariable UUID id) {
        return bookingService.cancelBooking(id);
    }
}