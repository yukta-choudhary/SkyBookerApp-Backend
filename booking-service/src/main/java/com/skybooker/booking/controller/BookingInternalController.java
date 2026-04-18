package com.skybooker.booking.controller;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Internal-only endpoints consumed by other microservices (notification-service scheduler).
 * Not exposed in Swagger as user-facing API.
 */
@RestController
@RequestMapping("/api/v1/bookings/internal")
@RequiredArgsConstructor
public class BookingInternalController {

    private final BookingRepository bookingRepository;

    /**
     * Returns confirmed bookings departing within a specific time window.
     * Used by notification-service to send check-in reminders.
     *
     * Example: GET /api/v1/bookings/internal/departing?from=2026-04-19T10:00:00&to=2026-04-19T12:00:00
     */
    @GetMapping("/departing")
    public List<Booking> getDepartingBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return bookingRepository.findConfirmedDepartingBetween(from, to);
    }
}
