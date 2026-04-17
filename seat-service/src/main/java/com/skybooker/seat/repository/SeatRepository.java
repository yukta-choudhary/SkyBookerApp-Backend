package com.skybooker.seat.repository;

import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.enums.SeatClass;
import com.skybooker.seat.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}