package com.skybooker.airline;

import com.skybooker.airline.dto.AirportRequest;
import com.skybooker.airline.entity.Airport;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirportRepository;
import com.skybooker.airline.service.impl.AirportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirportServiceImplTest {

    @Mock private AirportRepository airportRepository;

    @InjectMocks
    private AirportServiceImpl airportService;

    private Airport testAirport;
    private AirportRequest testRequest;

    @BeforeEach
    void setUp() {
        testAirport = Airport.builder()
                .airportId("airport-1")
                .name("Indira Gandhi International Airport")
                .iataCode("DEL")
                .icaoCode("VIDP")
                .city("Delhi")
                .country("India")
                .latitude(28.5562)
                .longitude(77.1000)
                .timezone("Asia/Kolkata")
                .createdAt(LocalDateTime.now())
                .build();

        testRequest = new AirportRequest();
        testRequest.setName("Indira Gandhi International Airport");
        testRequest.setIataCode("DEL");
        testRequest.setIcaoCode("VIDP");
        testRequest.setCity("Delhi");
        testRequest.setCountry("India");
        testRequest.setLatitude(28.5562);
        testRequest.setLongitude(77.1000);
        testRequest.setTimezone("Asia/Kolkata");
    }

    @Test
    @DisplayName("Should create airport successfully")
    void createAirport_success() {
        when(airportRepository.existsByIataCodeIgnoreCase("DEL")).thenReturn(false);
        when(airportRepository.save(any(Airport.class))).thenAnswer(inv -> inv.getArgument(0));

        Airport result = airportService.createAirport(testRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Indira Gandhi International Airport");
        assertThat(result.getIataCode()).isEqualTo("DEL");
        verify(airportRepository).save(any(Airport.class));
    }

    @Test
    @DisplayName("Should throw when IATA code already exists")
    void createAirport_duplicateIata() {
        when(airportRepository.existsByIataCodeIgnoreCase("DEL")).thenReturn(true);

        assertThatThrownBy(() -> airportService.createAirport(testRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IATA code already exists");
    }

    @Test
    @DisplayName("Should update airport successfully")
    void updateAirport_success() {
        testRequest.setName("Updated Airport Name");
        when(airportRepository.findById("airport-1")).thenReturn(Optional.of(testAirport));
        when(airportRepository.save(any(Airport.class))).thenAnswer(inv -> inv.getArgument(0));

        Airport result = airportService.updateAirport("airport-1", testRequest);

        assertThat(result.getName()).isEqualTo("Updated Airport Name");
    }

    @Test
    @DisplayName("Should get airport by ID")
    void getAirportById_success() {
        when(airportRepository.findById("airport-1")).thenReturn(Optional.of(testAirport));

        Airport result = airportService.getAirportById("airport-1");

        assertThat(result.getAirportId()).isEqualTo("airport-1");
    }

    @Test
    @DisplayName("Should throw when airport not found by ID")
    void getAirportById_notFound() {
        when(airportRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airportService.getAirportById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get airport by IATA code")
    void getAirportByIata_success() {
        when(airportRepository.findByIataCodeIgnoreCase("DEL")).thenReturn(Optional.of(testAirport));

        Airport result = airportService.getAirportByIata("DEL");

        assertThat(result.getIataCode()).isEqualTo("DEL");
    }

    @Test
    @DisplayName("Should get all airports")
    void getAllAirports_success() {
        when(airportRepository.findAll()).thenReturn(List.of(testAirport));

        List<Airport> result = airportService.getAllAirports();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should get airports by city")
    void getAirportsByCity_success() {
        when(airportRepository.findByCityContainingIgnoreCase("Delhi")).thenReturn(List.of(testAirport));

        List<Airport> result = airportService.getAirportsByCity("Delhi");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCity()).isEqualTo("Delhi");
    }

    @Test
    @DisplayName("Should get airports by country")
    void getAirportsByCountry_success() {
        when(airportRepository.findByCountryContainingIgnoreCase("India")).thenReturn(List.of(testAirport));

        List<Airport> result = airportService.getAirportsByCountry("India");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should search airports by keyword")
    void searchAirports_success() {
        when(airportRepository.findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrIataCodeContainingIgnoreCase(
                "Delhi", "Delhi", "Delhi")).thenReturn(List.of(testAirport));

        List<Airport> result = airportService.searchAirports("Delhi");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should delete airport")
    void deleteAirport_success() {
        when(airportRepository.findById("airport-1")).thenReturn(Optional.of(testAirport));

        airportService.deleteAirport("airport-1");

        verify(airportRepository).delete(testAirport);
    }
}
