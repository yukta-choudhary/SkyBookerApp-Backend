package com.skybooker.airline.service.impl;

import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirportRepository;
import com.skybooker.airline.service.AirportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AirportServiceImpl implements AirportService {

    private final AirportRepository airportRepository;

    @Override
    public Airport createAirport(AirportRequest request) {
        if (airportRepository.existsByIataCodeIgnoreCase(request.getIataCode())) {
            throw new IllegalArgumentException("Airport with this IATA code already exists");
        }

        Airport airport = Airport.builder()
                .name(request.getName())
                .iataCode(request.getIataCode())
                .icaoCode(request.getIcaoCode())
                .city(request.getCity())
                .country(request.getCountry())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .timezone(request.getTimezone())
                .build();

        return airportRepository.save(airport);
    }

    @Override
    public Airport updateAirport(String airportId, AirportRequest request) {
        Airport airport = getAirportById(airportId);

        airport.setName(request.getName());
        airport.setIataCode(request.getIataCode().toUpperCase());
        airport.setIcaoCode(request.getIcaoCode() == null ? null : request.getIcaoCode().toUpperCase());
        airport.setCity(request.getCity());
        airport.setCountry(request.getCountry());
        airport.setLatitude(request.getLatitude());
        airport.setLongitude(request.getLongitude());
        airport.setTimezone(request.getTimezone());

        return airportRepository.save(airport);
    }

    @Override
    public Airport getAirportById(String airportId) {
        return airportRepository.findById(airportId)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found"));
    }

    @Override
    public Airport getAirportByIata(String iataCode) {
        return airportRepository.findByIataCodeIgnoreCase(iataCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with IATA: " + iataCode));
    }

    @Override
    public List<Airport> getAllAirports() {
        return airportRepository.findAll();
    }

    @Override
    public List<Airport> getAirportsByCity(String city) {
        return airportRepository.findByCityContainingIgnoreCase(city);
    }

    @Override
    public List<Airport> getAirportsByCountry(String country) {
        return airportRepository.findByCountryContainingIgnoreCase(country);
    }

    @Override
    public List<Airport> searchAirports(String keyword) {
        return airportRepository.findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrIataCodeContainingIgnoreCase(
                keyword, keyword, keyword
        );
    }
}