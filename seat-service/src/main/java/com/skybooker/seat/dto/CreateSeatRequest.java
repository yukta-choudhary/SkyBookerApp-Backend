package com.skybooker.seat.dto;

import com.skybooker.seat.enums.SeatClass;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSeatRequest {

    @NotBlank
    private String seatNumber;

    @NotNull
    private SeatClass seatClass;

    @NotNull
    @Min(1)
    private Integer rowNumber;

    @NotBlank
    private String columnValue;

    private boolean isWindow;
    private boolean isAisle;
    private boolean hasExtraLegroom;

    @NotNull
    @DecimalMin("0.1")
    private Double priceMultiplier;
}