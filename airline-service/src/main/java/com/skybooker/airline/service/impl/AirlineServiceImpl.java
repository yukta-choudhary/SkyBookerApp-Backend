package com.skybooker.airline.service.impl;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirlineRepository;
import com.skybooker.airline.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;

    @Override
    public Airline createAirline(AirlineRequest request) {
        if (airlineRepository.existsByIataCodeIgnoreCase(request.getIataCode())) {
            throw new IllegalArgumentException("Airline with this IATA code already exists");
        }

        Airline airline = Airline.builder()
                .name(request.getName())
                .iataCode(request.getIataCode())
                .icaoCode(request.getIcaoCode())
                .logoUrl(request.getLogoUrl())
                .country(request.getCountry())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .isActive(request.getIsActive() == null ? true : request.getIsActive())
                .build();

        return airlineRepository.save(airline);
    }

    @Override
    public Airline updateAirline(String airlineId, AirlineRequest request) {
        Airline airline = getAirlineById(airlineId);

        airline.setName(request.getName());
        airline.setIataCode(request.getIataCode().toUpperCase());
        airline.setIcaoCode(request.getIcaoCode() == null ? null : request.getIcaoCode().toUpperCase());
        airline.setLogoUrl(request.getLogoUrl());
        airline.setCountry(request.getCountry());
        airline.setContactEmail(request.getContactEmail());
        airline.setContactPhone(request.getContactPhone());
        airline.setIsActive(request.getIsActive() == null ? airline.getIsActive() : request.getIsActive());

        return airlineRepository.save(airline);
    }

    @Override
    public Airline getAirlineById(String airlineId) {
        return airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found"));
    }

    @Override
    public Airline getAirlineByIata(String iataCode) {
        return airlineRepository.findByIataCodeIgnoreCase(iataCode)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with IATA: " + iataCode));
    }

    @Override
    public List<Airline> getAllAirlines() {
        return airlineRepository.findAll();
    }

    @Override
    public List<Airline> getActiveAirlines() {
        return airlineRepository.findByIsActiveTrue();
    }

    @Override
    public Airline deactivateAirline(String airlineId) {
        Airline airline = getAirlineById(airlineId);
        airline.setIsActive(false);
        return airlineRepository.save(airline);
    }

    @Override
    public Airline activateAirline(String airlineId) {
        Airline airline = getAirlineById(airlineId);
        airline.setIsActive(true);
        return airlineRepository.save(airline);
    }
}