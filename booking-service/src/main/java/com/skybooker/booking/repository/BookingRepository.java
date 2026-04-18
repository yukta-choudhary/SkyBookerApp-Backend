package com.skybooker.booking.repository;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserId(UUID userId);

    Optional<Booking> findByPnrCode(String pnrCode);

    List<Booking> findByFlightId(UUID flightId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status);

    /** Upcoming bookings: PENDING or CONFIRMED with future departure */
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId " +
           "AND b.status IN ('PENDING','CONFIRMED') " +
           "AND (b.departureTime IS NULL OR b.departureTime > :now)")
    List<Booking> findUpcomingByUserId(UUID userId, LocalDateTime now);

    /** Bookings departing within a time window — for check-in reminder scheduler */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.departureTime BETWEEN :windowStart AND :windowEnd")
    List<Booking> findConfirmedDepartingBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

    /** Confirmed bookings whose departure has passed — candidates for NO_SHOW */
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.departureTime IS NOT NULL AND b.departureTime < :cutoff")
    List<Booking> findConfirmedPastDeparture(LocalDateTime cutoff);
}