package com.skybooker.airline.service;

import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.entity.Airport;

import java.util.List;

public interface AirportService {
    Airport createAirport(AirportRequest request);
    Airport updateAirport(String airportId, AirportRequest request);
    Airport getAirportById(String airportId);
    Airport getAirportByIata(String iataCode);
    List<Airport> getAllAirports();
    List<Airport> getAirportsByCity(String city);
    List<Airport> getAirportsByCountry(String country);
    List<Airport> searchAirports(String keyword);
    void deleteAirport(String airportId);
}
