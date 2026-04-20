package com.skybooker.booking.controller;

import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Flight booking lifecycle: create, view, cancel, confirm, and manage add-ons")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Create Booking", description = "Create a new flight booking. A PNR code is generated automatically. Status is set to PENDING until payment is verified.")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking created with PENDING status"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "PASSENGER role required")
    })
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public Booking create(@RequestBody Booking booking) {
        return bookingService.createBooking(booking);
    }

    @Operation(summary = "Get Booking by ID", description = "Fetch a booking by UUID. Passengers can only view their own bookings.")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking found"),
        @ApiResponse(responseCode = "403", description = "Access denied — not your booking"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public Booking getById(@Parameter(description = "Booking UUID") @PathVariable UUID id) {
        return bookingService.getBookingById(id);
    }

    @Operation(summary = "Get Booking by PNR", description = "Look up a booking by its 6-character PNR code. Public endpoint for check-in kiosks.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking found"),
        @ApiResponse(responseCode = "404", description = "PNR not found")
    })
    @GetMapping("/pnr/{pnr}")
    public Booking getByPnr(@Parameter(description = "6-character PNR code") @PathVariable String pnr) {
        return bookingService.getBookingByPnr(pnr);
    }

    @Operation(summary = "Get Bookings by User", description = "List all bookings for a user. Passengers can only view their own.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public List<Booking> getByUser(@Parameter(description = "User UUID") @PathVariable UUID userId) {
        return bookingService.getBookingsByUser(userId);
    }

    @Operation(summary = "Get Bookings by Flight", description = "List all bookings for a specific flight. Used by airline staff for manifest generation.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/flight/{flightId}")
    @PreAuthorize("hasAnyRole('AIRLINE_STAFF','ADMIN')")
    public List<Booking> getByFlight(@Parameter(description = "Flight UUID") @PathVariable UUID flightId) {
        return bookingService.getBookingsByFlight(flightId);
    }

    @Operation(summary = "Get Upcoming Bookings", description = "List upcoming (future departure) bookings for a user.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/user/{userId}/upcoming")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<List<Booking>> getUpcoming(@PathVariable UUID userId) {
        return ResponseEntity.ok(bookingService.getUpcomingBookings(userId));
    }

    @Operation(summary = "Cancel Booking", description = "Cancel a booking. Passengers can only cancel their own. Sets status to CANCELLED.")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled"),
        @ApiResponse(responseCode = "403", description = "Not your booking"),
        @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public Booking cancel(@PathVariable UUID id) {
        return bookingService.cancelBooking(id);
    }

    // Called internally by payment-service after successful payment
    @Operation(summary = "Confirm Booking (Internal)", description = "Called by payment-service after Razorpay verification. Sets status to CONFIRMED. Idempotent.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public Booking confirm(@PathVariable UUID id) {
        return bookingService.confirmBooking(id);
    }

    @Operation(
        summary = "Add Add-On to Booking",
        description = "Add meal preference, extra luggage, or additional charges to an existing booking. Cannot modify CANCELLED or COMPLETED bookings."
    )
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Add-on applied"),
        @ApiResponse(responseCode = "400", description = "Cannot modify cancelled/completed booking"),
        @ApiResponse(responseCode = "403", description = "Not your booking")
    })
    @PostMapping("/{id}/addon")
    @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public ResponseEntity<Booking> addAddOn(
            @Parameter(description = "Booking UUID") @PathVariable UUID id,
            @RequestBody AddOnRequest request) {
        return ResponseEntity.ok(bookingService.addAddOn(id, request));
    }
}