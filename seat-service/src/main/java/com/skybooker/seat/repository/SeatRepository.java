package com.skybooker.seat.repository;

import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.enums.SeatClass;
import com.skybooker.seat.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByFlightId(UUID flightId);

    List<Seat> findByFlightIdAndSeatClass(UUID flightId, SeatClass seatClass);

    List<Seat> findByFlightIdAndStatus(UUID flightId, SeatStatus status);

    List<Seat> findByFlightIdAndSeatClassAndStatus(UUID flightId, SeatClass seatClass, SeatStatus status);

    Optional<Seat> findByFlightIdAndSeatNumber(UUID flightId, String seatNumber);

    long countByFlightIdAndSeatClassAndStatus(UUID flightId, SeatClass seatClass, SeatStatus status);

    void deleteByFlightId(UUID flightId);

    void deleteByFlightIdAndSeatClass(UUID flightId, SeatClass seatClass);

    /**
     * Efficiently find only HELD seats whose hold has expired, avoiding loading all seats.
     */
    List<Seat> findByStatusAndHoldExpiresAtBefore(SeatStatus status, LocalDateTime now);

    /**
     * Bulk-update expired holds to AVAILABLE in a single DB query for efficiency.
     */
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.holdExpiresAt = NULL " +
           "WHERE s.status = 'HELD' AND s.holdExpiresAt IS NOT NULL AND s.holdExpiresAt < :now")
    int releaseExpiredHolds(@Param("now") LocalDateTime now);
}
