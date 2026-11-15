package com.TicketBooking.Scheduler;

import com.TicketBooking.Service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {

    private final BookingService bookingService;

    // Every minute, release any seat holds whose 10-minute window has
    // lapsed without payment, so they go back to AVAILABLE for others.
    @Scheduled(fixedRate = 60000)
    public void releaseExpiredHolds() {
        try {
            bookingService.releaseExpiredHolds();
        } catch (Exception e) {
            log.error("Failed to release expired booking holds", e);
        }
    }
}
