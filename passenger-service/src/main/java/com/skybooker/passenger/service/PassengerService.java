package com.skybooker.passenger.service;

import com.skybooker.passenger.entity.PassengerInfo;

import java.util.List;
import java.util.UUID;

public interface PassengerService {

    PassengerInfo addPassenger(PassengerInfo passenger);

    PassengerInfo getPassengerById(UUID passengerId);

    List<PassengerInfo> getPassengersByBooking(UUID bookingId);

    PassengerInfo updatePassenger(UUID passengerId, PassengerInfo passenger);

    PassengerInfo assignSeat(UUID passengerId, UUID seatId, String seatNumber);

    void deletePassenger(UUID passengerId);

    long getPassengerCount(UUID bookingId);
}