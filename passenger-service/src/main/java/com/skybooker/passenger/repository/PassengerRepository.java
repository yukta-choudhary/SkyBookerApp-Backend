package com.skybooker.passenger.repository;

import com.skybooker.passenger.entity.PassengerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PassengerRepository extends JpaRepository<PassengerInfo, UUID> {

    List<PassengerInfo> findByBookingId(UUID bookingId);

    Optional<PassengerInfo> findByPassportNumber(String passportNumber);

    Optional<PassengerInfo> findByTicketNumber(String ticketNumber);

    List<PassengerInfo> findBySeatId(UUID seatId);

    long countByBookingId(UUID bookingId);

    void deleteByBookingId(UUID bookingId);
}