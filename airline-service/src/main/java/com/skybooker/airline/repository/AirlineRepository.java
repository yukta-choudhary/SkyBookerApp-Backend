package com.skybooker.airline.repository;

import com.skybooker.airline.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirlineRepository extends JpaRepository<Airline, String> {
    Optional<Airline> findByIataCodeIgnoreCase(String iataCode);
    Optional<Airline> findByIcaoCodeIgnoreCase(String icaoCode);
    List<Airline> findByIsActiveTrue();
    boolean existsByIataCodeIgnoreCase(String iataCode);
}