package com.skybooker.airline.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AirlineRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 2, max = 3)
    private String iataCode;

    @Size(max = 4)
    private String icaoCode;

    private String logoUrl;

    @NotBlank
    private String country;

    @Email
    private String contactEmail;

    private String contactPhone;

    private Boolean isActive;
}