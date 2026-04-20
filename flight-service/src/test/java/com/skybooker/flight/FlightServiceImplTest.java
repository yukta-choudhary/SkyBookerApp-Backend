package com.skybooker.flight;

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

/**
 * Unit tests for FlightServiceImpl.
 * Covers flight CRUD, search, status updates, and Kafka publishing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlightServiceImpl Unit Tests")
class FlightServiceImplTest {

    @Mock private FlightRepository flightRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private FlightServiceImpl flightService;

    private UUID flightId;
    private UUID airlineId;
    private Flight testFlight;

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
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .durationMinutes(120)
                .status(FlightStatus.ON_TIME)
                .aircraftType("Boeing 737")
                .totalSeats(180)
                .availableSeats(150)
                .basePrice(4500.0)
                .build();
    }

    // ===== ADD FLIGHT TESTS =====

    @Test
    @DisplayName("AddFlight: should save flight with ON_TIME status when status is null")
    void addFlight_shouldSetDefaultStatus_whenStatusNull() {
        testFlight.setStatus(null);
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> {
            Flight f = inv.getArgument(0);
            assertThat(f.getStatus()).isEqualTo(FlightStatus.ON_TIME);
            return f;
        });

        flightService.addFlight(testFlight);

        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    @DisplayName("AddFlight: should set availableSeats to totalSeats on creation")
    void addFlight_shouldSetAvailableSeats_toTotalSeats() {
        testFlight.setAvailableSeats(null);
        testFlight.setTotalSeats(200);
        when(flightRepository.save(any(Flight.class))).thenReturn(testFlight);

        Flight saved = flightService.addFlight(testFlight);

        // Should set availableSeats = totalSeats before saving
        assertThat(testFlight.getAvailableSeats()).isEqualTo(200);
    }

    // ===== GET FLIGHT TESTS =====

    @Test
    @DisplayName("GetFlightById: should return flight for valid ID")
    void getFlightById_shouldReturnFlight() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));

        Flight result = flightService.getFlightById(flightId);

        assertThat(result.getFlightNumber()).isEqualTo("SK101");
        assertThat(result.getOriginAirportCode()).isEqualTo("DEL");
    }

    @Test
    @DisplayName("GetFlightById: should throw RuntimeException for unknown ID")
    void getFlightById_shouldThrow_whenNotFound() {
        when(flightRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.getFlightById(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flight not found");
    }

    // ===== GET FLIGHTS BY AIRLINE =====

    @Test
    @DisplayName("GetFlightsByAirline: should return list of flights for airline")
    void getFlightsByAirline_shouldReturnList() {
        when(flightRepository.findByAirlineId(airlineId)).thenReturn(List.of(testFlight));

        List<Flight> flights = flightService.getFlightsByAirline(airlineId);

        assertThat(flights).hasSize(1);
        assertThat(flights.get(0).getAirlineId()).isEqualTo(airlineId);
    }

    @Test
    @DisplayName("GetFlightsByAirline: should return empty list when no flights")
    void getFlightsByAirline_shouldReturnEmpty_whenNoneExist() {
        when(flightRepository.findByAirlineId(airlineId)).thenReturn(List.of());

        List<Flight> flights = flightService.getFlightsByAirline(airlineId);

        assertThat(flights).isEmpty();
    }

    // ===== SEARCH FLIGHTS =====

    @Test
    @DisplayName("SearchFlights: should return matching flights for origin-dest-date")
    void searchFlights_shouldReturnFlights() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("DEL"), eq("BOM"), any(), any()))
                .thenReturn(List.of(testFlight));

        List<Flight> results = flightService.searchFlights("del", "bom", date);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFlightNumber()).isEqualTo("SK101");
    }

    @Test
    @DisplayName("SearchFlights: should convert origin/dest to uppercase before querying")
    void searchFlights_shouldUppercaseAirportCodes() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("DEL"), eq("BOM"), any(), any()))
                .thenReturn(List.of());

        flightService.searchFlights("del", "bom", date);

        verify(flightRepository).findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("DEL"), eq("BOM"), any(), any());
    }

    @Test
    @DisplayName("SearchFlights: should return empty list when no flights on that date")
    void searchFlights_shouldReturnEmpty_whenNoFlights() {
        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                anyString(), anyString(), any(), any()))
                .thenReturn(List.of());

        List<Flight> results = flightService.searchFlights("DEL", "BOM", LocalDate.now().plusDays(3));

        assertThat(results).isEmpty();
    }

    // ===== ROUND TRIP SEARCH =====

    @Test
    @DisplayName("SearchRoundTrip: should return outbound and return flights")
    void searchRoundTrip_shouldReturnBothLegs() {
        Flight returnFlight = Flight.builder()
                .flightId(UUID.randomUUID())
                .flightNumber("SK201")
                .airlineId(airlineId)
                .originAirportCode("BOM")
                .destinationAirportCode("DEL")
                .status(FlightStatus.ON_TIME)
                .build();

        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("DEL"), eq("BOM"), any(), any()))
                .thenReturn(List.of(testFlight));
        when(flightRepository.findByOriginAirportCodeAndDestinationAirportCodeAndDepartureTimeBetween(
                eq("BOM"), eq("DEL"), any(), any()))
                .thenReturn(List.of(returnFlight));

        var rt = flightService.searchRoundTrip("DEL", "BOM",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(7));

        assertThat(rt.getOutboundFlights()).hasSize(1);
        assertThat(rt.getReturnFlights()).hasSize(1);
        assertThat(rt.getOutboundFlights().get(0).getFlightNumber()).isEqualTo("SK101");
        assertThat(rt.getReturnFlights().get(0).getFlightNumber()).isEqualTo("SK201");
    }

    // ===== UPDATE STATUS TESTS =====

    @Test
    @DisplayName("UpdateStatus: should change status and publish Kafka event")
    void updateStatus_shouldChangeStatusAndPublishEvent() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));
        when(flightRepository.save(any(Flight.class))).thenReturn(testFlight);

        Flight result = flightService.updateStatus(flightId, "DELAYED");

        assertThat(result.getStatus()).isEqualTo(FlightStatus.DELAYED);
        verify(kafkaTemplate).send(eq("flight-status-changed"), anyString(), any());
    }

    @Test
    @DisplayName("UpdateStatus: should not throw when Kafka is unavailable")
    void updateStatus_shouldNotThrow_whenKafkaFails() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));
        when(flightRepository.save(any(Flight.class))).thenReturn(testFlight);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        // Should NOT throw — Kafka is best-effort
        assertThatCode(() -> flightService.updateStatus(flightId, "CANCELLED"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UpdateStatus: should throw for invalid status value")
    void updateStatus_shouldThrow_forInvalidStatus() {
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));

        assertThatThrownBy(() -> flightService.updateStatus(flightId, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== UPDATE FLIGHT =====

    @Test
    @DisplayName("UpdateFlight: should update all fields and save")
    void updateFlight_shouldUpdateAllFields() {
        Flight updated = Flight.builder()
                .flightNumber("SK999")
                .airlineId(airlineId)
                .originAirportCode("CCU")
                .destinationAirportCode("HYD")
                .durationMinutes(90)
                .totalSeats(150)
                .availableSeats(100)
                .basePrice(3000.0)
                .build();

        when(flightRepository.findById(flightId)).thenReturn(Optional.of(testFlight));
        when(flightRepository.save(any())).thenReturn(testFlight);

        Flight result = flightService.updateFlight(flightId, updated);

        verify(flightRepository).save(any());
        assertThat(testFlight.getFlightNumber()).isEqualTo("SK999");
        assertThat(testFlight.getOriginAirportCode()).isEqualTo("CCU");
    }

    // ===== DELETE FLIGHT =====

    @Test
    @DisplayName("DeleteFlight: should call deleteById on repository")
    void deleteFlight_shouldDeleteById() {
        doNothing().when(flightRepository).deleteById(flightId);

        flightService.deleteFlight(flightId);

        verify(flightRepository).deleteById(flightId);
    }
}
