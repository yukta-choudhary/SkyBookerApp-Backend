package com.skybooker.flight.dto;

import com.skybooker.flight.entity.Flight;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundTripResponse {
    private List<Flight> outboundFlights;
    private List<Flight> returnFlights;
}
