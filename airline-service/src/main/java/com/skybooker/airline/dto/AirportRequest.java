package com.skybooker.airline.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AirportRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 3, max = 3)
    private String iataCode;

    @Size(max = 4)
    private String icaoCode;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotBlank
    private String timezone;
}