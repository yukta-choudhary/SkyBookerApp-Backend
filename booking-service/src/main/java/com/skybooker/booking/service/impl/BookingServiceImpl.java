package com.skybooker.booking.service.impl;

import com.skybooker.booking.config.SecurityUtils;
import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.enums.BookingStatus;
import com.skybooker.booking.repository.BookingRepository;
import com.skybooker.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    public Booking createBooking(Booking booking) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required to create a booking");
        }
        booking.setBookingId(null);
        booking.setUserId(currentUserId);
        booking.setBookedAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPnrCode(generatePnr());

        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (SecurityUtils.hasRole("PASSENGER") && !booking.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You can only view your own bookings");
        }

        return booking;
    }

    @Override
    public Booking getBookingByPnr(String pnr) {
        return bookingRepository.findByPnrCode(pnr)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(UUID userId) {
        if (SecurityUtils.hasRole("PASSENGER") && !userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You can only view your own bookings");
        }
        return bookingRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getUpcomingBookings(UUID userId) {
        if (SecurityUtils.hasRole("PASSENGER") && !userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You can only view your own bookings");
        }
        return bookingRepository.findUpcomingByUserId(userId, LocalDateTime.now());
    }

    @Override
    public List<Booking> getBookingsByFlight(UUID flightId) {
        return bookingRepository.findByFlightId(flightId);
    }

    @Override
    public Booking cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (SecurityUtils.hasRole("PASSENGER") && !booking.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You can only cancel your own booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Override
    public Booking confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking; // idempotent
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    @Override
    public Booking addAddOn(UUID bookingId, AddOnRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (SecurityUtils.hasRole("PASSENGER") &&
                !booking.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You can only modify your own booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED ||
                booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot modify a " + booking.getStatus() + " booking");
        }

        if (request.getMealPreference() != null) {
            booking.setMealPreference(request.getMealPreference());
        }
        if (request.getExtraLuggageKg() != null) {
            double current = booking.getLuggageKg() != null ? booking.getLuggageKg() : 0.0;
            booking.setLuggageKg(current + request.getExtraLuggageKg());
        }
        if (request.getAdditionalCost() != null && request.getAdditionalCost() > 0) {
            double current = booking.getTotalFare() != null ? booking.getTotalFare() : 0.0;
            booking.setTotalFare(current + request.getAdditionalCost());
        }

        return bookingRepository.save(booking);
    }

    private String generatePnr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}