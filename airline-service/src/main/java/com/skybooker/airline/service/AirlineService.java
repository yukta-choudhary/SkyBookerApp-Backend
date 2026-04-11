package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.entity.Airline;

import java.util.List;

public interface AirlineService {
    Airline createAirline(AirlineRequest request);
    Airline updateAirline(String airlineId, AirlineRequest request);
    Airline getAirlineById(String airlineId);
    Airline getAirlineByIata(String iataCode);
    List<Airline> getAllAirlines();
    List<Airline> getActiveAirlines();
    Airline deactivateAirline(String airlineId);
    Airline activateAirline(String airlineId);
}