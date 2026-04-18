package com.skybooker.seat.service.impl;

import com.skybooker.seat.dto.AddSeatsForFlightRequest;
import com.skybooker.seat.dto.CreateSeatRequest;
import com.skybooker.seat.dto.UpdateSeatRequest;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.enums.SeatClass;
import com.skybooker.seat.enums.SeatStatus;
import com.skybooker.seat.repository.SeatRepository;
import com.skybooker.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;

    @Override
    public List<Seat> addSeatsForFlight(AddSeatsForFlightRequest request) {
        List<Seat> seats = request.getSeats().stream()
                .map(seatRequest -> mapToSeat(request.getFlightId(), seatRequest))
                .toList();

        return seatRepository.saveAll(seats);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(UUID flightId) {
        return seatRepository.findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> getAvailableByClass(UUID flightId, SeatClass seatClass) {
        return seatRepository.findByFlightIdAndSeatClassAndStatus(flightId, seatClass, SeatStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public Seat getSeatById(UUID seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
    }

    @Override
    public Seat holdSeat(UUID seatId) {
        Seat seat = getSeatById(seatId);

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new RuntimeException("Seat is not available for hold");
        }

        seat.setStatus(SeatStatus.HELD);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));

        try {
            return seatRepository.save(seat);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new RuntimeException("Seat was modified by another request. Please try again.");
        }
    }

    @Override
    public Seat releaseSeat(UUID seatId) {
        Seat seat = getSeatById(seatId);

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new RuntimeException("Only held seats can be released");
        }

        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setHoldExpiresAt(null);
        return seatRepository.save(seat);
    }

    @Override
    public Seat confirmSeat(UUID seatId) {
        Seat seat = getSeatById(seatId);

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new RuntimeException("Only held seats can be confirmed");
        }

        if (seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            throw new RuntimeException("Seat hold expired");
        }

        seat.setStatus(SeatStatus.CONFIRMED);
        seat.setHoldExpiresAt(null);
        return seatRepository.save(seat);
    }

    @Override
    public Seat updateSeat(UUID seatId, UpdateSeatRequest request) {
        Seat seat = getSeatById(seatId);

        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatClass(request.getSeatClass());
        seat.setRowNumber(request.getRowNumber());
        seat.setColumnValue(request.getColumnValue());
        seat.setWindowSeat(request.isWindowSeat());
        seat.setAisleSeat(request.isAisleSeat());
        seat.setHasExtraLegroom(request.isHasExtraLegroom());
        seat.setStatus(request.getStatus());
        seat.setPriceMultiplier(request.getPriceMultiplier());

        if (request.getStatus() != SeatStatus.HELD) {
            seat.setHoldExpiresAt(null);
        }

        return seatRepository.save(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> getSeatMap(UUID flightId) {
        return seatRepository.findByFlightId(flightId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAvailableByClass(UUID flightId, SeatClass seatClass) {
        return seatRepository.countByFlightIdAndSeatClassAndStatus(flightId, seatClass, SeatStatus.AVAILABLE);
    }

    @Override
    public void deleteSeatsForFlight(UUID flightId) {
        seatRepository.deleteByFlightId(flightId);
    }

    @Override
    @Scheduled(fixedRate = 120000)
    public void releaseExpiredHolds() {
        int released = seatRepository.releaseExpiredHolds(LocalDateTime.now());
        if (released > 0) {
            // log if needed
        }
    }

    private Seat mapToSeat(UUID flightId, CreateSeatRequest request) {
        return Seat.builder()
                .flightId(flightId)
                .seatNumber(request.getSeatNumber())
                .seatClass(request.getSeatClass())
                .rowNumber(request.getRowNumber())
                .columnValue(request.getColumnValue())
                .windowSeat(request.isWindowSeat())
                .aisleSeat(request.isAisleSeat())
                .hasExtraLegroom(request.isHasExtraLegroom())
                .status(SeatStatus.AVAILABLE)
                .priceMultiplier(request.getPriceMultiplier())
                .holdExpiresAt(null)
                .build();
    }
}