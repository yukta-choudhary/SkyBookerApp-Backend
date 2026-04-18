package com.skybooker.seat.dto;

import com.skybooker.seat.enums.SeatClass;
import com.skybooker.seat.enums.SeatStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSeatRequest {

    @NotBlank
    private String seatNumber;

    @NotNull
    private SeatClass seatClass;

    @NotNull
    @Min(1)
    private Integer rowNumber;

    @NotBlank
    private String columnValue;

    private boolean windowSeat;
    private boolean aisleSeat;
    private boolean hasExtraLegroom;

    @NotNull
    private SeatStatus status;

    @NotNull
    @DecimalMin("0.1")
    private Double priceMultiplier;
}