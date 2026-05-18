package com.skybooker.flight;

import com.skybooker.flight.dto.RoundTripResponse;
import com.skybooker.flight.entity.Flight;
import com.skybooker.flight.enums.FlightStatus;
import com.skybooker.flight.repository.FlightRepository;
import com.skybooker.flight.service.impl.FlightServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock private FlightRepository flightRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private FlightServiceImpl flightService;

    private Flight testFlight;
    private UUID flightId;
    private UUID airlineId;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();
        airlineId = UUID.randomUUID();

        testFlight = Flight.builder()
                .flightId(flightId)
                .flightNumber("SK101")
                .airlineId(airlineId)
                .originAirportCode("DEL")
                .destinationAirportCode("BOM")
                .departureTime(LocalDateTime.of(2026, 6, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 6, 1, 12, 30))
                .totalSeats(180)
                .availableSeats(180)
                .basePrice(5000.0)
                .status(FlightStatus.ON_TIME)
                .build();
    }

    @Test
    @DisplayName("Should add flight and set available seats and duration")
    void addFlight_success() {
        Flight newFlight = Flight.builder()
                .flightNumber("SK102")
                .totalSeats(200)
                .departureTime(LocalDateTime.of(2026, 6, 1, 14, 0))
                .arrivalTime(LocalDateTime.of(2026, 6, 1, 16, 0))
                .build();

        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight result = flightService.addFlight(newFlight);

        assertThat(result.getAvailableSeats()).isEqualTo(200);
        assertThat(result.getDurationMinutes()).isEqualTo(120);
        assertThat(result.getStatus()).isEqualTo(FlightStatus.ON_TIME);
    }

    @Test
    @DisplayName("Should add flight with explicit status")
    void addFlight_withExplicitStatus() {
        Flight newFlight = Flight.builder()
                .flightNumber("SK103")
                .totalSeats(100)
                .departureTime(LocalDateTime.of(2026, 6, 1, 14, 0))
                .arrivalTime(LocalDateTime.of(2026, 6, 1, 16, 0))
                .status(FlightStatus.DELAYED)
                .build();

        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight result = flightService.addFlight(newFlight);

        assertThat(result.getStatus()).isEqualTo(FlightStatus.DELAYED);
    }

    @Test
    @DisplayName("Should throw when arrival is before departure")
    void addFlight_invalidTimes() {
        Flight invalidFlight = Flight.builder()
                .totalSeats(100)
                .departureTime(LocalDateTime.of(2026, 6, 1, 16, 0))
                .arrivalTime(LocalDateTime.of(2026, 6, 1, 14, 0))
                .build();

        assertThatThrownBy(() -> flightService.addFlight(invalidFlight))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Arrival time must be after departure time");
    }

    @Test
    @DisplayName("Should get flight by ID")
    void getFlightById_success() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));

        Flight result = flightService.getFlightById(flightId);

        assertThat(result.getFlightNumber()).isEqualTo("SK101");
    }

    @Test
    @DisplayName("Should throw when flight not found")
    void getFlightById_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(flightRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.getFlightById(unknownId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    @Test
    @DisplayName("Should get flights by airline ID")
    void getFlightsByAirline_success() {
        when(flightRepository.findByAirlineId(airlineId)).thenReturn(List.of(testFlight));

        List<Flight> result = flightService.getFlightsByAirline(airlineId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should search flights by origin, destination and date")
    void searchFlights_success() {
        LocalDate date = LocalDate.of(2026, 6, 1);
        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("DEL"), eq("BOM"), any(), any())).thenReturn(List.of(testFlight));

        List<Flight> result = flightService.searchFlights("DEL", "BOM", date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginAirportCode()).isEqualTo("DEL");
    }

    @Test
    @DisplayName("Should search round-trip flights")
    void searchRoundTrip_success() {
        LocalDate depDate = LocalDate.of(2026, 6, 1);
        LocalDate retDate = LocalDate.of(2026, 6, 5);

        Flight returnFlight = Flight.builder()
                .flightNumber("SK102")
                .originAirportCode("BOM")
                .destinationAirportCode("DEL")
                .build();

        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("DEL"), eq("BOM"), any(), any())).thenReturn(List.of(testFlight));
        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("BOM"), eq("DEL"), any(), any())).thenReturn(List.of(returnFlight));

        RoundTripResponse result = flightService.searchRoundTrip("DEL", "BOM", depDate, retDate);

        assertThat(result.getOutboundFlights()).hasSize(1);
        assertThat(result.getReturnFlights()).hasSize(1);
    }

    @Test
    @DisplayName("Should update flight successfully")
    void updateFlight_success() {
        Flight updated = Flight.builder()
                .flightNumber("SK101-UPDATED")
                .airlineId(airlineId)
                .originAirportCode("DEL")
                .destinationAirportCode("BLR")
                .departureTime(LocalDateTime.of(2026, 6, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 6, 1, 13, 0))
                .totalSeats(200)
                .availableSeats(200)
                .basePrice(6000.0)
                .build();

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight result = flightService.updateFlight(flightId, updated);

        assertThat(result.getFlightNumber()).isEqualTo("SK101-UPDATED");
        assertThat(result.getDestinationAirportCode()).isEqualTo("BLR");
        assertThat(result.getDurationMinutes()).isEqualTo(180);
    }

    @Test
    @DisplayName("Should update flight status and publish Kafka event")
    void updateStatus_success() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        Flight result = flightService.updateStatus(flightId, "DELAYED");

        assertThat(result.getStatus()).isEqualTo(FlightStatus.DELAYED);
        verify(kafkaTemplate).send(eq("flight-status-changed"), anyString(), any());
    }

    @Test
    @DisplayName("Should update status even when Kafka publish fails")
    void updateStatus_kafkaFails() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("Kafka down"));

        Flight result = flightService.updateStatus(flightId, "CANCELLED");

        assertThat(result.getStatus()).isEqualTo(FlightStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should delete flight")
    void deleteFlight_success() {
        flightService.deleteFlight(flightId);

        verify(flightRepository).deleteById(flightId);
    }
}
