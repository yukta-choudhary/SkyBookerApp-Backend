package com.skybooker.airline;

import com.skybooker.airline.dto.AirlineRequest;
import com.skybooker.airline.entity.Airline;
import com.skybooker.airline.exception.ResourceNotFoundException;
import com.skybooker.airline.repository.AirlineRepository;
import com.skybooker.airline.service.impl.AirlineServiceImpl;
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
class AirlineServiceImplTest {

    @Mock private AirlineRepository airlineRepository;

    @InjectMocks
    private AirlineServiceImpl airlineService;

    private Airline testAirline;
    private AirlineRequest testRequest;

    @BeforeEach
    void setUp() {
        testAirline = Airline.builder()
                .airlineId("airline-1")
                .name("SkyBooker Airlines")
                .iataCode("SB")
                .icaoCode("SKB")
                .country("India")
                .contactEmail("contact@skybooker.com")
                .contactPhone("1234567890")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        testRequest = new AirlineRequest();
        testRequest.setName("SkyBooker Airlines");
        testRequest.setIataCode("SB");
        testRequest.setIcaoCode("SKB");
        testRequest.setCountry("India");
        testRequest.setContactEmail("contact@skybooker.com");
        testRequest.setContactPhone("1234567890");
        testRequest.setIsActive(true);
    }

    @Test
    @DisplayName("Should create airline successfully")
    void createAirline_success() {
        when(airlineRepository.existsByIataCodeIgnoreCase("SB")).thenReturn(false);
        when(airlineRepository.save(any(Airline.class))).thenAnswer(inv -> inv.getArgument(0));

        Airline result = airlineService.createAirline(testRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("SkyBooker Airlines");
        assertThat(result.getIataCode()).isEqualTo("SB");
        verify(airlineRepository).save(any(Airline.class));
    }

    @Test
    @DisplayName("Should throw when IATA code already exists")
    void createAirline_duplicateIata() {
        when(airlineRepository.existsByIataCodeIgnoreCase("SB")).thenReturn(true);

        assertThatThrownBy(() -> airlineService.createAirline(testRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IATA code already exists");
    }

    @Test
    @DisplayName("Should update airline successfully")
    void updateAirline_success() {
        testRequest.setName("Updated Airlines");
        when(airlineRepository.findById("airline-1")).thenReturn(Optional.of(testAirline));
        when(airlineRepository.save(any(Airline.class))).thenAnswer(inv -> inv.getArgument(0));

        Airline result = airlineService.updateAirline("airline-1", testRequest);

        assertThat(result.getName()).isEqualTo("Updated Airlines");
    }

    @Test
    @DisplayName("Should get airline by ID")
    void getAirlineById_success() {
        when(airlineRepository.findById("airline-1")).thenReturn(Optional.of(testAirline));

        Airline result = airlineService.getAirlineById("airline-1");

        assertThat(result.getAirlineId()).isEqualTo("airline-1");
    }

    @Test
    @DisplayName("Should throw when airline not found by ID")
    void getAirlineById_notFound() {
        when(airlineRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.getAirlineById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get airline by IATA code")
    void getAirlineByIata_success() {
        when(airlineRepository.findByIataCodeIgnoreCase("SB")).thenReturn(Optional.of(testAirline));

        Airline result = airlineService.getAirlineByIata("SB");

        assertThat(result.getIataCode()).isEqualTo("SB");
    }

    @Test
    @DisplayName("Should throw when airline not found by IATA")
    void getAirlineByIata_notFound() {
        when(airlineRepository.findByIataCodeIgnoreCase("XX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.getAirlineByIata("XX"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all airlines")
    void getAllAirlines_success() {
        when(airlineRepository.findAll()).thenReturn(List.of(testAirline));

        List<Airline> result = airlineService.getAllAirlines();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should get active airlines only")
    void getActiveAirlines_success() {
        when(airlineRepository.findByIsActiveTrue()).thenReturn(List.of(testAirline));

        List<Airline> result = airlineService.getActiveAirlines();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should deactivate airline")
    void deactivateAirline_success() {
        when(airlineRepository.findById("airline-1")).thenReturn(Optional.of(testAirline));
        when(airlineRepository.save(any(Airline.class))).thenAnswer(inv -> inv.getArgument(0));

        Airline result = airlineService.deactivateAirline("airline-1");

        assertThat(result.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should activate airline")
    void activateAirline_success() {
        testAirline.setIsActive(false);
        when(airlineRepository.findById("airline-1")).thenReturn(Optional.of(testAirline));
        when(airlineRepository.save(any(Airline.class))).thenAnswer(inv -> inv.getArgument(0));

        Airline result = airlineService.activateAirline("airline-1");

        assertThat(result.getIsActive()).isTrue();
    }
}
