package com.skybooker.seat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_flight_seat_number", columnNames = {"flight_id", "seat_number"})
        }
)
public class Seat {

    @Id
    @GeneratedValue
    @Column(name = "seat_id")
    private UUID seatId;

    @Column(name = "flight_id")
    private UUID flightId;

    @Column(name = "seat_number")
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class")
    private SeatClass seatClass;

    @Column(name = "seat_row")
    private Integer rowNumber;

    @Column(name = "seat_column")
    private String columnValue;

    @Column(name = "window_seat")
    private boolean windowSeat;

    @Column(name = "aisle_seat")
    private boolean aisleSeat;

    @Column(name = "has_extra_legroom")
    private boolean hasExtraLegroom;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status")
    private SeatStatus status;

    @Column(name = "price_multiplier")
    private Double priceMultiplier;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @JsonIgnore
    @Version
    @Column(name = "version")
    private Long version;
}