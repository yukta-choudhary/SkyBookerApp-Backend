package com.skybooker.booking.service;

import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.entity.Booking;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    Booking createBooking(Booking booking);

    Booking getBookingById(UUID bookingId);

    Booking getBookingByPnr(String pnr);

    List<Booking> getBookingsByUser(UUID userId);

    /** Returns PENDING/CONFIRMED bookings with future departure for the My Bookings dashboard */
    List<Booking> getUpcomingBookings(UUID userId);

    List<Booking> getBookingsByFlight(UUID flightId);

    Booking cancelBooking(UUID bookingId);

    Booking confirmBooking(UUID bookingId);

    /** Adds/updates meal preference and/or extra baggage to an existing booking */
    Booking addAddOn(UUID bookingId, AddOnRequest request);
}