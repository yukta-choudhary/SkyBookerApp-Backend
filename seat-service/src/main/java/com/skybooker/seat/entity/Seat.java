package com.skybooker.seat.entity;

import com.skybooker.seat.enums.SeatClass;
import com.skybooker.seat.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_flight_seat_number", columnNames = {"flightId", "seatNumber"})
        }
)
public class Seat {

    @Id
    @GeneratedValue
    private UUID seatId;

    private UUID flightId;

    private String seatNumber;

    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;

    @Column(name = "seat_row")
    private Integer rowNumber;

    @Column(name = "seat_column")
    private String columnValue;

    private boolean isWindow;
    private boolean isAisle;
    private boolean hasExtraLegroom;

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    private Double priceMultiplier;

    private LocalDateTime holdExpiresAt;

    @Version
    private Long version;
}