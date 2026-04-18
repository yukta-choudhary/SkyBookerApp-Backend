package com.skybooker.booking.entity;

import com.skybooker.booking.enums.BookingStatus;
import com.skybooker.booking.enums.TripType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue
    private UUID bookingId;

    private UUID userId;
    private UUID flightId;

    private String pnrCode;

    @Enumerated(EnumType.STRING)
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    private BookingStatus status;

    private Double totalFare;
    private Double baseFare;
    private Double taxes;

    private String mealPreference;
    private Double luggageKg;

    private String contactEmail;
    private String contactPhone;

    private LocalDateTime bookedAt;

    private UUID paymentId;
}