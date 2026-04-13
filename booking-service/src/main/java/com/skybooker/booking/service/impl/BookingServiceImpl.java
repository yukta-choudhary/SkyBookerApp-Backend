package com.skybooker.booking.service.impl;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.enums.BookingStatus;
import com.skybooker.booking.repository.BookingRepository;
import com.skybooker.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    public Booking createBooking(Booking booking) {

        booking.setBookedAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPnrCode(generatePnr());

        return bookingRepository.save(booking);
    }

    @Override
    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    @Override
    public Booking getBookingByPnr(String pnr) {
        return bookingRepository.findByPnrCode(pnr)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    @Override
    public List<Booking> getBookingsByUser(UUID userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getBookingsByFlight(UUID flightId) {
        return bookingRepository.findByFlightId(flightId);
    }

    @Override
    public Booking cancelBooking(UUID bookingId) {

        Booking booking = getBookingById(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    private String generatePnr() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}