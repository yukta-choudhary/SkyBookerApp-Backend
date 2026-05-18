package com.skybooker.booking;

import com.skybooker.booking.dto.AddOnRequest;
import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.enums.BookingStatus;
import com.skybooker.booking.repository.BookingRepository;
import com.skybooker.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID userId;
    private UUID flightId;
    private UUID bookingId;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        flightId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        testBooking = Booking.builder()
                .bookingId(bookingId)
                .userId(userId)
                .flightId(flightId)
                .pnrCode("ABC123")
                .status(BookingStatus.PENDING)
                .totalFare(5000.0)
                .baseFare(4500.0)
                .taxes(500.0)
                .bookedAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(UUID uid, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                uid.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ─── Create Booking ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should create booking with generated PNR and PENDING status")
    void createBooking_success() {
        setSecurityContext(userId, "PASSENGER");
        Booking input = Booking.builder().flightId(flightId).totalFare(5000.0).build();

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setBookingId(bookingId);
            return b;
        });

        Booking result = bookingService.createBooking(input);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getPnrCode()).isNotNull();
        assertThat(result.getPnrCode()).hasSize(6);
        assertThat(result.getBookedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw when no authentication for create booking")
    void createBooking_noAuth() {
        SecurityContextHolder.clearContext();
        Booking input = Booking.builder().build();

        assertThatThrownBy(() -> bookingService.createBooking(input))
                .isInstanceOf(Exception.class);
    }

    // ─── Get Booking by ID ───────────────────────────────────────────────

    @Test
    @DisplayName("Should get booking by ID for admin/staff")
    void getBookingById_admin() {
        setSecurityContext(UUID.randomUUID(), "ADMIN");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        Booking result = bookingService.getBookingById(bookingId);

        assertThat(result.getBookingId()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("Should get booking by ID for own passenger")
    void getBookingById_ownPassenger() {
        setSecurityContext(userId, "PASSENGER");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        Booking result = bookingService.getBookingById(bookingId);

        assertThat(result.getBookingId()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("Should throw when passenger tries to view another's booking")
    void getBookingById_otherPassenger() {
        setSecurityContext(UUID.randomUUID(), "PASSENGER");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.getBookingById(bookingId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should throw when booking not found")
    void getBookingById_notFound() {
        setSecurityContext(userId, "PASSENGER");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(bookingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found");
    }

    // ─── Get Booking by PNR ──────────────────────────────────────────────

    @Test
    @DisplayName("Should get booking by PNR code")
    void getBookingByPnr_success() {
        when(bookingRepository.findByPnrCode("ABC123")).thenReturn(Optional.of(testBooking));

        Booking result = bookingService.getBookingByPnr("ABC123");

        assertThat(result.getPnrCode()).isEqualTo("ABC123");
    }

    // ─── Get Bookings by User ────────────────────────────────────────────

    @Test
    @DisplayName("Should get bookings by user for admin")
    void getBookingsByUser_admin() {
        setSecurityContext(UUID.randomUUID(), "ADMIN");
        when(bookingRepository.findByUserId(userId)).thenReturn(List.of(testBooking));

        List<Booking> result = bookingService.getBookingsByUser(userId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should throw when passenger tries to get another user's bookings")
    void getBookingsByUser_otherPassenger() {
        setSecurityContext(UUID.randomUUID(), "PASSENGER");

        assertThatThrownBy(() -> bookingService.getBookingsByUser(userId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    // ─── Get Bookings by Flight ──────────────────────────────────────────

    @Test
    @DisplayName("Should get bookings by flight ID")
    void getBookingsByFlight_success() {
        when(bookingRepository.findByFlightId(flightId)).thenReturn(List.of(testBooking));

        List<Booking> result = bookingService.getBookingsByFlight(flightId);

        assertThat(result).hasSize(1);
    }

    // ─── Cancel Booking ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should cancel booking for own passenger")
    void cancelBooking_success() {
        setSecurityContext(userId, "PASSENGER");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should throw when passenger cancels another's booking")
    void cancelBooking_accessDenied() {
        setSecurityContext(UUID.randomUUID(), "PASSENGER");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    // ─── Confirm Booking ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should confirm pending booking")
    void confirmBooking_success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.confirmBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should return same booking if already confirmed (idempotent)")
    void confirmBooking_alreadyConfirmed() {
        testBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        Booking result = bookingService.confirmBooking(bookingId);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, never()).save(any());
    }

    // ─── Add-On ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should add meal preference and extra luggage")
    void addAddOn_success() {
        setSecurityContext(userId, "PASSENGER");
        testBooking.setLuggageKg(15.0);
        testBooking.setTotalFare(5000.0);

        AddOnRequest request = new AddOnRequest();
        request.setMealPreference("VEG");
        request.setExtraLuggageKg(5.0);
        request.setAdditionalCost(500.0);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.addAddOn(bookingId, request);

        assertThat(result.getMealPreference()).isEqualTo("VEG");
        assertThat(result.getLuggageKg()).isEqualTo(20.0);
        assertThat(result.getTotalFare()).isEqualTo(5500.0);
    }

    @Test
    @DisplayName("Should throw when adding add-on to cancelled booking")
    void addAddOn_cancelledBooking() {
        setSecurityContext(userId, "PASSENGER");
        testBooking.setStatus(BookingStatus.CANCELLED);

        AddOnRequest request = new AddOnRequest();
        request.setMealPreference("VEG");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));

        assertThatThrownBy(() -> bookingService.addAddOn(bookingId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot modify");
    }
}
