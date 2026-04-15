package com.skybooker.passenger.service.impl;

import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.repository.PassengerRepository;
import com.skybooker.passenger.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;

    @Override
    public PassengerInfo addPassenger(PassengerInfo passenger) {
        passenger.setTicketNumber(generateTicketNumber());
        return passengerRepository.save(passenger);
    }

    @Override
    public PassengerInfo getPassengerById(UUID passengerId) {
        return passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));
    }

    @Override
    public List<PassengerInfo> getPassengersByBooking(UUID bookingId) {
        return passengerRepository.findByBookingId(bookingId);
    }

    @Override
    public PassengerInfo updatePassenger(UUID passengerId, PassengerInfo updated) {

        PassengerInfo passenger = getPassengerById(passengerId);

        passenger.setFirstName(updated.getFirstName());
        passenger.setLastName(updated.getLastName());
        passenger.setPassportNumber(updated.getPassportNumber());

        return passengerRepository.save(passenger);
    }

    @Override
    public PassengerInfo assignSeat(UUID passengerId, UUID seatId, String seatNumber) {

        PassengerInfo passenger = getPassengerById(passengerId);
        passenger.setSeatId(seatId);
        passenger.setSeatNumber(seatNumber);

        return passengerRepository.save(passenger);
    }

    @Override
    public void deletePassenger(UUID passengerId) {
        passengerRepository.deleteById(passengerId);
    }

    @Override
    public long getPassengerCount(UUID bookingId) {
        return passengerRepository.countByBookingId(bookingId);
    }

    private String generateTicketNumber() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}