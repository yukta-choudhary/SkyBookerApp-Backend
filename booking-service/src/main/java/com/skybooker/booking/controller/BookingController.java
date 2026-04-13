package com.skybooker.booking.controller;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public Booking create(@RequestBody Booking booking) {
        return bookingService.createBooking(booking);
    }

    @GetMapping("/{id}")
    public Booking getById(@PathVariable UUID id) {
        return bookingService.getBookingById(id);
    }

    @GetMapping("/pnr/{pnr}")
    public Booking getByPnr(@PathVariable String pnr) {
        return bookingService.getBookingByPnr(pnr);
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getByUser(@PathVariable UUID userId) {
        return bookingService.getBookingsByUser(userId);
    }

    @GetMapping("/flight/{flightId}")
    public List<Booking> getByFlight(@PathVariable UUID flightId) {
        return bookingService.getBookingsByFlight(flightId);
    }

    @PutMapping("/{id}/cancel")
    public Booking cancel(@PathVariable UUID id) {
        return bookingService.cancelBooking(id);
    }
}