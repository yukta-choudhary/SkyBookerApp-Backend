package com.skybooker.passenger.entity;

import com.skybooker.passenger.enums.PassengerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "passenger_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerInfo {

    @Id
    @GeneratedValue
    private UUID passengerId;

    private UUID userId;
    private UUID bookingId;

    private String title;
    private String firstName;
    private String lastName;

    private LocalDate dateOfBirth;
    private String gender;

    private String passportNumber;
    private String nationality;
    private LocalDate passportExpiry;

    private UUID seatId;
    private String seatNumber;

    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    private PassengerType passengerType;
}