package com.skybooker.seat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddSeatsForFlightRequest {

    @NotNull
    private UUID flightId;

    @Valid
    @NotEmpty
    private List<CreateSeatRequest> seats;
}