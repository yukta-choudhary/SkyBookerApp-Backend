package com.skybooker.passenger.service.impl;

import com.skybooker.passenger.config.SecurityUtils;
import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.repository.PassengerRepository;
import com.skybooker.passenger.service.PassengerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;

    @Override
    public PassengerInfo addPassenger(PassengerInfo passenger) {
        passenger.setPassengerId(null);
        passenger.setUserId(SecurityUtils.getCurrentUserId());
        passenger.setTicketNumber(generateTicketNumber());
        return passengerRepository.save(passenger);
    }

    @Override
    public PassengerInfo getPassengerById(UUID passengerId) {
        PassengerInfo passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));

        if (SecurityUtils.hasRole("PASSENGER") && !passenger.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You can only view your own passenger records");
        }

        return passenger;
    }

    @Override
    public List<PassengerInfo> getPassengersByBooking(UUID bookingId) {
        if (SecurityUtils.hasRole("PASSENGER")) {
            return passengerRepository.findByBookingIdAndUserId(bookingId, SecurityUtils.getCurrentUserId());
        }
        return passengerRepository.findByBookingId(bookingId);
    }

    @Override
    public PassengerInfo updatePassenger(UUID passengerId, PassengerInfo updated) {
        PassengerInfo passenger = getPassengerById(passengerId);

        passenger.setTitle(updated.getTitle());
        passenger.setFirstName(updated.getFirstName());
        passenger.setLastName(updated.getLastName());
        passenger.setDateOfBirth(updated.getDateOfBirth());
        passenger.setGender(updated.getGender());
        passenger.setPassportNumber(updated.getPassportNumber());
        passenger.setNationality(updated.getNationality());
        passenger.setPassportExpiry(updated.getPassportExpiry());
        passenger.setPassengerType(updated.getPassengerType());

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
        PassengerInfo passenger = getPassengerById(passengerId);
        passengerRepository.delete(passenger);
    }

    @Override
    public long getPassengerCount(UUID bookingId) {
        return passengerRepository.countByBookingId(bookingId);
    }

    private String generateTicketNumber() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}