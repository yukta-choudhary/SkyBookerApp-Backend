package com.skybooker.seat.service;

import com.skybooker.seat.dto.AddSeatsForFlightRequest;
import com.skybooker.seat.dto.UpdateSeatRequest;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.enums.SeatClass;

import java.util.List;
import java.util.UUID;

public interface SeatService {

    List<Seat> addSeatsForFlight(AddSeatsForFlightRequest request);

    List<Seat> getAvailableSeats(UUID flightId);

    List<Seat> getAvailableByClass(UUID flightId, SeatClass seatClass);

    Seat getSeatById(UUID seatId);

    Seat holdSeat(UUID seatId);

    Seat releaseSeat(UUID seatId);

    Seat confirmSeat(UUID seatId);

    Seat updateSeat(UUID seatId, UpdateSeatRequest request);

    List<Seat> getSeatMap(UUID flightId);

    long countAvailableByClass(UUID flightId, SeatClass seatClass);

    void deleteSeatsForFlight(UUID flightId);

    void releaseExpiredHolds();
}