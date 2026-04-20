package com.skybooker.booking;

import com.skybooker.booking.config.SecurityUtils;
import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.enums.BookingStatus;
import com.skybooker.booking.enums.TripType;
import com.skybooker.booking.repository.BookingRepository;
import com.skybooker.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingServiceImpl.
 * Uses MockedStatic to mock SecurityUtils.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl Unit Tests")
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;

    @InjectMocks private BookingServiceImpl bookingService;

    private UUID bookingId;
    private UUID userId;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testBooking = Booking.builder()
                .bookingId(bookingId)
                .userId(userId)
                .flightId(UUID.randomUUID())
                .tripType(TripType.ONE_WAY)
                .status(BookingStatus.PENDING)
                .totalFare(5500.0)
                .baseFare(5000.0)
                .taxes(500.0)
                .pnrCode("ABCDEF")
                .bookedAt(LocalDateTime.now())
                .build();
    }

    // ===== CREATE BOOKING TESTS =====

    @Test
    @DisplayName("CreateBooking: should create booking with PENDING status and PNR")
    void createBooking_shouldSucceed_withValidUser() {
        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            Booking newBooking = Booking.builder()
                    .flightId(testBooking.getFlightId())
                    .tripType(TripType.ONE_WAY)
                    .totalFare(5500.0)
                    .build();

            Booking result = bookingService.createBooking(newBooking);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
            verify(bookingRepository).save(any(Booking.class));
        }
    }

    @Test
    @DisplayName("CreateBooking: should throw AccessDeniedException when no authenticated user")
    void createBooking_shouldThrow_whenNoCurrentUser() {
        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(SecurityUtils::getCurrentUserId).thenReturn(null);

            assertThatThrownBy(() -> bookingService.createBooking(testBooking))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Authentication required");
        }
    }

    // ===== GET BOOKING BY ID =====

    @Test
    @DisplayName("GetBookingById: should return booking for ADMIN role")
    void getBookingById_shouldReturnBooking_forAdmin() {
        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(false);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

            Booking result = bookingService.getBookingById(bookingId);

            assertThat(result.getBookingId()).isEqualTo(bookingId);
            assertThat(result.getPnrCode()).isEqualTo("ABCDEF");
        }
    }

    @Test
    @DisplayName("GetBookingById: should throw for unknown booking ID")
    void getBookingById_shouldThrow_whenNotFound() {
        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(false);
            when(bookingRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBookingById(UUID.randomUUID()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Booking not found");
        }
    }

    @Test
    @DisplayName("GetBookingById: should throw AccessDeniedException when PASSENGER views other's booking")
    void getBookingById_shouldThrow_whenPassengerViewsOtherBooking() {
        UUID otherUserId = UUID.randomUUID();
        testBooking.setUserId(otherUserId); // booking belongs to someone else

        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(true);
            mock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

            assertThatThrownBy(() -> bookingService.getBookingById(bookingId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own bookings");
        }
    }

    // ===== GET BOOKING BY PNR =====

    @Test
    @DisplayName("GetBookingByPnr: should return booking for valid PNR")
    void getBookingByPnr_shouldReturnBooking() {
        when(bookingRepository.findByPnrCode("ABCDEF")).thenReturn(Optional.of(testBooking));

        Booking result = bookingService.getBookingByPnr("ABCDEF");

        assertThat(result.getPnrCode()).isEqualTo("ABCDEF");
    }

    @Test
    @DisplayName("GetBookingByPnr: should throw for unknown PNR")
    void getBookingByPnr_shouldThrow_whenPnrNotFound() {
        when(bookingRepository.findByPnrCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingByPnr("XXXXXX"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    // ===== CANCEL BOOKING =====

    @Test
    @DisplayName("CancelBooking: should set status to CANCELLED")
    void cancelBooking_shouldSetCancelledStatus() {
        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(false);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any())).thenReturn(testBooking);

            Booking result = bookingService.cancelBooking(bookingId);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    @Test
    @DisplayName("CancelBooking: should throw when PASSENGER tries to cancel another's booking")
    void cancelBooking_shouldThrow_whenPassengerCancelsOtherBooking() {
        UUID otherUser = UUID.randomUUID();
        testBooking.setUserId(otherUser);

        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(true);
            mock.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

            assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ===== CONFIRM BOOKING =====

    @Test
    @DisplayName("ConfirmBooking: should set status to CONFIRMED")
    void confirmBooking_shouldSetConfirmedStatus() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any())).thenReturn(testBooking);

        Booking result = bookingService.confirmBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("ConfirmBooking: should be idempotent — no re-save when already CONFIRMED")
    void confirmBooking_shouldBeIdempotent_whenAlreadyConfirmed() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        Booking result = bookingService.confirmBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("ConfirmBooking: should throw when booking not found")
    void confirmBooking_shouldThrow_whenNotFound() {
        when(bookingRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBooking(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    // ===== ADD-ON TESTS =====

    @Test
    @DisplayName("AddAddOn: should update meal preference and luggage")
    void addAddOn_shouldUpdateMealAndLuggage() {
        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(false);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
            when(bookingRepository.save(any())).thenReturn(testBooking);

            AddOnRequest req = new AddOnRequest();
            req.setMealPreference("VEGETARIAN");
            req.setExtraLuggageKg(10.0);
            req.setAdditionalCost(500.0);

            Booking result = bookingService.addAddOn(bookingId, req);

            assertThat(testBooking.getMealPreference()).isEqualTo("VEGETARIAN");
            assertThat(testBooking.getLuggageKg()).isEqualTo(10.0);
            assertThat(testBooking.getTotalFare()).isEqualTo(6000.0); // 5500 + 500
        }
    }

    @Test
    @DisplayName("AddAddOn: should throw when booking is CANCELLED")
    void addAddOn_shouldThrow_whenBookingCancelled() {
        testBooking.setStatus(BookingStatus.CANCELLED);

        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(false);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

            AddOnRequest req = new AddOnRequest();
            req.setMealPreference("VEGAN");

            assertThatThrownBy(() -> bookingService.addAddOn(bookingId, req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("CANCELLED");
        }
    }

    // ===== GET BOOKINGS BY USER =====

    @Test
    @DisplayName("GetBookingsByUser: should return list for ADMIN")
    void getBookingsByUser_shouldReturnList_forAdmin() {
        try (MockedStatic<SecurityUtils> mock = mockStatic(SecurityUtils.class)) {
            mock.when(() -> SecurityUtils.hasRole("PASSENGER")).thenReturn(false);
            when(bookingRepository.findByUserId(userId)).thenReturn(List.of(testBooking));

            List<Booking> bookings = bookingService.getBookingsByUser(userId);

            assertThat(bookings).hasSize(1);
        }
    }

    // ===== GET BOOKINGS BY FLIGHT =====

    @Test
    @DisplayName("GetBookingsByFlight: should return all bookings for a flight")
    void getBookingsByFlight_shouldReturnList() {
        UUID flightId = testBooking.getFlightId();
        when(bookingRepository.findByFlightId(flightId)).thenReturn(List.of(testBooking));

        List<Booking> result = bookingService.getBookingsByFlight(flightId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlightId()).isEqualTo(flightId);
    }
}
