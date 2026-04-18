package com.skybooker.booking.scheduler;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.enums.BookingStatus;
import com.skybooker.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detects confirmed bookings whose departure time has passed (gate closure) —
 * transitions them to NO_SHOW unless they have already been COMPLETED or CANCELLED.
 *
 * Runs every 30 minutes. Marks bookings as NO_SHOW if departure was > 1 hour ago.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowDetectionScheduler {

    private final BookingRepository bookingRepository;

    /** 1 hour after scheduled departure we consider the gate closed */
    private static final long GATE_CLOSURE_HOURS = 1;

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT5M")
    @Transactional
    public void detectNoShows() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(GATE_CLOSURE_HOURS);
        List<Booking> candidates = bookingRepository.findConfirmedPastDeparture(cutoff);

        if (candidates.isEmpty()) return;

        log.info("No-show scheduler: {} candidate(s) found past gate closure", candidates.size());

        for (Booking booking : candidates) {
            booking.setStatus(BookingStatus.NO_SHOW);
            bookingRepository.save(booking);
            log.info("Marked NO_SHOW: bookingId={} pnr={} departure={}",
                    booking.getBookingId(), booking.getPnrCode(), booking.getDepartureTime());
        }

        log.info("No-show scheduler: {} booking(s) transitioned to NO_SHOW", candidates.size());
    }
}
