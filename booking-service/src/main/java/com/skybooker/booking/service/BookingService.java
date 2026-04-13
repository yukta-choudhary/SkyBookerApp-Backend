package com.skybooker.booking.service;

import com.skybooker.booking.entity.Booking;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    Booking createBooking(Booking booking);

    Booking getBookingById(UUID bookingId);

    Booking getBookingByPnr(String pnr);

    List<Booking> getBookingsByUser(UUID userId);

    List<Booking> getBookingsByFlight(UUID flightId);

    Booking cancelBooking(UUID bookingId);
}