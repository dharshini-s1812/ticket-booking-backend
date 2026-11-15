package com.TicketBooking.Scheduler;

import com.TicketBooking.Service.StreamingMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieExpiryScheduler {

    private final StreamingMovieService streamingMovieService;

    // Once a day is enough — the check is date-based (LocalDate), not
    // time-based, so running more often wouldn't change anything until the
    // calendar day actually rolls over. initialDelay = 0 means it also runs
    // right at startup, so a warning/removal that fell due while the server
    // was down still fires promptly instead of waiting up to 24h.
    @Scheduled(initialDelay = 0, fixedRate = 24 * 60 * 60 * 1000)
    public void checkMovieSchedules() {
        try {
            streamingMovieService.processScheduledExpiries();
        } catch (Exception e) {
            log.error("Failed to process movie schedule expiries", e);
        }
    }
}
