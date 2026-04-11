package com.skybooker.airline.repository;

import com.skybooker.airline.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirportRepository extends JpaRepository<Airport, String> {
    Optional<Airport> findByIataCodeIgnoreCase(String iataCode);
    List<Airport> findByCityContainingIgnoreCase(String city);
    List<Airport> findByCountryContainingIgnoreCase(String country);
    List<Airport> findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrIataCodeContainingIgnoreCase(
            String name, String city, String iataCode
    );
    boolean existsByIataCodeIgnoreCase(String iataCode);
}