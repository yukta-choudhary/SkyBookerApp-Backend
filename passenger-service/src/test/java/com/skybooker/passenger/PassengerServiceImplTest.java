package com.skybooker.passenger;

import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.enums.PassengerType;
import com.skybooker.passenger.repository.PassengerRepository;
import com.skybooker.passenger.service.impl.PassengerServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerServiceImplTest {

    @Mock private PassengerRepository passengerRepository;
    @InjectMocks private PassengerServiceImpl passengerService;

    private UUID userId, passengerId, bookingId, seatId;
    private PassengerInfo testPassenger;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        passengerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        testPassenger = PassengerInfo.builder()
                .passengerId(passengerId).userId(userId).bookingId(bookingId)
                .title("Mr").firstName("John").lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1)).gender("Male")
                .passportNumber("AB123456").nationality("Indian")
                .passengerType(PassengerType.ADULT)
                .build();
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private void setAuth(UUID uid, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(uid.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test @DisplayName("Should add passenger")
    void addPassenger() {
        setAuth(userId, "PASSENGER");
        PassengerInfo input = PassengerInfo.builder().firstName("Jane").lastName("Doe").build();
        when(passengerRepository.save(any())).thenAnswer(i -> {
            PassengerInfo p = i.getArgument(0);
            p.setPassengerId(passengerId);
            return p;
        });
        PassengerInfo res = passengerService.addPassenger(input);
        assertThat(res.getUserId()).isEqualTo(userId);
        assertThat(res.getTicketNumber()).isNotNull().hasSize(8);
    }

    @Test @DisplayName("Should throw when no auth for add")
    void addPassenger_noAuth() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> passengerService.addPassenger(PassengerInfo.builder().build()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test @DisplayName("Should get passenger by ID for admin")
    void getById_admin() {
        setAuth(UUID.randomUUID(), "ADMIN");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(testPassenger));
        assertThat(passengerService.getPassengerById(passengerId).getFirstName()).isEqualTo("John");
    }

    @Test @DisplayName("Should get own passenger for PASSENGER role")
    void getById_ownPassenger() {
        setAuth(userId, "PASSENGER");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(testPassenger));
        assertThat(passengerService.getPassengerById(passengerId)).isNotNull();
    }

    @Test @DisplayName("Should throw when passenger views another's record")
    void getById_otherPassenger() {
        setAuth(UUID.randomUUID(), "PASSENGER");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(testPassenger));
        assertThatThrownBy(() -> passengerService.getPassengerById(passengerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test @DisplayName("Should throw when passenger not found")
    void getById_notFound() {
        setAuth(userId, "PASSENGER");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> passengerService.getPassengerById(passengerId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should get passengers by booking for admin")
    void getByBooking_admin() {
        setAuth(UUID.randomUUID(), "ADMIN");
        when(passengerRepository.findByBookingId(bookingId)).thenReturn(List.of(testPassenger));
        assertThat(passengerService.getPassengersByBooking(bookingId)).hasSize(1);
    }

    @Test @DisplayName("Should get own passengers by booking for PASSENGER")
    void getByBooking_passenger() {
        setAuth(userId, "PASSENGER");
        when(passengerRepository.findByBookingIdAndUserId(bookingId, userId)).thenReturn(List.of(testPassenger));
        assertThat(passengerService.getPassengersByBooking(bookingId)).hasSize(1);
    }

    @Test @DisplayName("Should update passenger")
    void updatePassenger() {
        setAuth(userId, "PASSENGER");
        PassengerInfo updated = PassengerInfo.builder()
                .title("Mrs").firstName("Jane").lastName("Smith")
                .dateOfBirth(LocalDate.of(1992, 5, 15)).gender("Female")
                .passportNumber("CD789").nationality("Indian")
                .passengerType(PassengerType.ADULT).build();
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(testPassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        PassengerInfo res = passengerService.updatePassenger(passengerId, updated);
        assertThat(res.getFirstName()).isEqualTo("Jane");
        assertThat(res.getTitle()).isEqualTo("Mrs");
    }

    @Test @DisplayName("Should assign seat")
    void assignSeat() {
        setAuth(userId, "PASSENGER");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(testPassenger));
        when(passengerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        PassengerInfo res = passengerService.assignSeat(passengerId, seatId, "12A");
        assertThat(res.getSeatId()).isEqualTo(seatId);
        assertThat(res.getSeatNumber()).isEqualTo("12A");
    }

    @Test @DisplayName("Should delete passenger")
    void deletePassenger() {
        setAuth(userId, "PASSENGER");
        when(passengerRepository.findById(passengerId)).thenReturn(Optional.of(testPassenger));
        passengerService.deletePassenger(passengerId);
        verify(passengerRepository).delete(testPassenger);
    }

    @Test @DisplayName("Should get passenger count")
    void getCount() {
        when(passengerRepository.countByBookingId(bookingId)).thenReturn(3L);
        assertThat(passengerService.getPassengerCount(bookingId)).isEqualTo(3);
    }
}
