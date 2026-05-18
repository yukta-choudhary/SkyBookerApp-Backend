package com.skybooker.seat;

import com.skybooker.seat.dto.AddSeatsForFlightRequest;
import com.skybooker.seat.dto.CreateSeatRequest;
import com.skybooker.seat.dto.UpdateSeatRequest;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.enums.SeatClass;
import com.skybooker.seat.enums.SeatStatus;
import com.skybooker.seat.repository.SeatRepository;
import com.skybooker.seat.service.impl.SeatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock private SeatRepository seatRepository;
    @InjectMocks private SeatServiceImpl seatService;

    private UUID flightId, seatId;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        flightId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        testSeat = Seat.builder()
                .seatId(seatId).flightId(flightId).seatNumber("1A")
                .seatClass(SeatClass.ECONOMY).rowNumber(1).columnValue("A")
                .windowSeat(true).status(SeatStatus.AVAILABLE).priceMultiplier(1.0)
                .build();
    }

    @Test @DisplayName("Should add seats for flight")
    void addSeats() {
        CreateSeatRequest sr = new CreateSeatRequest();
        sr.setSeatNumber("1A"); sr.setSeatClass(SeatClass.ECONOMY);
        sr.setRowNumber(1); sr.setColumnValue("A"); sr.setPriceMultiplier(1.0);
        AddSeatsForFlightRequest req = new AddSeatsForFlightRequest();
        req.setFlightId(flightId); req.setSeats(List.of(sr));
        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        List<Seat> res = seatService.addSeatsForFlight(req);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test @DisplayName("Should get available seats")
    void getAvailable() {
        when(seatRepository.findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE)).thenReturn(List.of(testSeat));
        assertThat(seatService.getAvailableSeats(flightId)).hasSize(1);
    }

    @Test @DisplayName("Should get available by class")
    void getAvailableByClass() {
        when(seatRepository.findByFlightIdAndSeatClassAndStatus(flightId, SeatClass.ECONOMY, SeatStatus.AVAILABLE))
                .thenReturn(List.of(testSeat));
        assertThat(seatService.getAvailableByClass(flightId, SeatClass.ECONOMY)).hasSize(1);
    }

    @Test @DisplayName("Should get seat by ID")
    void getSeatById() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        assertThat(seatService.getSeatById(seatId).getSeatNumber()).isEqualTo("1A");
    }

    @Test @DisplayName("Should throw when seat not found")
    void seatNotFound() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> seatService.getSeatById(seatId)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should hold available seat")
    void holdSeat() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Seat res = seatService.holdSeat(seatId);
        assertThat(res.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(res.getHoldExpiresAt()).isNotNull();
    }

    @Test @DisplayName("Should throw hold on non-available seat")
    void holdNotAvailable() {
        testSeat.setStatus(SeatStatus.HELD);
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        assertThatThrownBy(() -> seatService.holdSeat(seatId)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should throw on optimistic lock")
    void holdOptimisticLock() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any())).thenThrow(new ObjectOptimisticLockingFailureException(Seat.class, seatId));
        assertThatThrownBy(() -> seatService.holdSeat(seatId)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should release held seat")
    void releaseSeat() {
        testSeat.setStatus(SeatStatus.HELD);
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Seat res = seatService.releaseSeat(seatId);
        assertThat(res.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test @DisplayName("Should throw release on non-held")
    void releaseNotHeld() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        assertThatThrownBy(() -> seatService.releaseSeat(seatId)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should confirm held seat")
    void confirmSeat() {
        testSeat.setStatus(SeatStatus.HELD);
        testSeat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Seat res = seatService.confirmSeat(seatId);
        assertThat(res.getStatus()).isEqualTo(SeatStatus.CONFIRMED);
    }

    @Test @DisplayName("Should throw confirm on non-held")
    void confirmNotHeld() {
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        assertThatThrownBy(() -> seatService.confirmSeat(seatId)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should throw when hold expired on confirm")
    void confirmExpired() {
        testSeat.setStatus(SeatStatus.HELD);
        testSeat.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThatThrownBy(() -> seatService.confirmSeat(seatId)).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Should update seat")
    void updateSeat() {
        UpdateSeatRequest req = UpdateSeatRequest.builder()
                .seatNumber("2B").seatClass(SeatClass.BUSINESS).rowNumber(2).columnValue("B")
                .aisleSeat(true).hasExtraLegroom(true).status(SeatStatus.AVAILABLE).priceMultiplier(1.5).build();
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(testSeat));
        when(seatRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Seat res = seatService.updateSeat(seatId, req);
        assertThat(res.getSeatNumber()).isEqualTo("2B");
        assertThat(res.getSeatClass()).isEqualTo(SeatClass.BUSINESS);
    }

    @Test @DisplayName("Should get seat map")
    void getSeatMap() {
        when(seatRepository.findByFlightId(flightId)).thenReturn(List.of(testSeat));
        assertThat(seatService.getSeatMap(flightId)).hasSize(1);
    }

    @Test @DisplayName("Should count available by class")
    void countAvailable() {
        when(seatRepository.countByFlightIdAndSeatClassAndStatus(flightId, SeatClass.ECONOMY, SeatStatus.AVAILABLE)).thenReturn(50L);
        assertThat(seatService.countAvailableByClass(flightId, SeatClass.ECONOMY)).isEqualTo(50);
    }

    @Test @DisplayName("Should delete seats for flight")
    void deleteSeats() {
        seatService.deleteSeatsForFlight(flightId);
        verify(seatRepository).deleteByFlightId(flightId);
    }

    @Test @DisplayName("Should release expired holds")
    void releaseExpired() {
        when(seatRepository.releaseExpiredHolds(any())).thenReturn(3);
        seatService.releaseExpiredHolds();
        verify(seatRepository).releaseExpiredHolds(any());
    }
}
