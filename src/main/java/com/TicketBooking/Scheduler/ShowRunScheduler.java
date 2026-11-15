package com.TicketBooking.Scheduler;

import com.TicketBooking.Model.NotificationType;
import com.TicketBooking.Model.Show;
import com.TicketBooking.Model.ShowRun;
import com.TicketBooking.Repository.ShowRepository;
import com.TicketBooking.Repository.ShowRunRepository;
import com.TicketBooking.Service.NotificationService;
import com.TicketBooking.Service.ShowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowRunScheduler {

    private final ShowRunRepository showRunRepository;
    private final ShowRepository showRepository;
    private final ShowService showService;
    private final NotificationService notificationService;

    // Once a day at 1 AM — this isn't time-sensitive to the minute, and
    // running it more often would just mean re-checking the same "is it
    // tomorrow yet" condition for no benefit.
    private static final String DAILY_CRON = "0 0 1 * * *";

    // Runs whose current endDate is tomorrow: prompt the admin once to
    // extend (3/5/7 days) or let it end. Only fires once per endDate — see
    // ShowRun.lastExtensionPromptForEndDate — so ignoring it doesn't nag
    // daily, and extending resets it so the NEW endDate gets its own prompt.
    @Scheduled(cron = DAILY_CRON)
    @Transactional
    public void promptEndingSoonRuns() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            List<ShowRun> endingSoon = showRunRepository.findByActiveTrueAndEndDate(tomorrow);

            for (ShowRun run : endingSoon) {
                if (tomorrow.equals(run.getLastExtensionPromptForEndDate())) {
                    continue; // already prompted for this endDate
                }
                notificationService.notifyAdmins(
                        NotificationType.RUN_ENDING_SOON,
                        "Show run ending tomorrow",
                        "\"" + run.getMovie().getMovieName() + "\" on " + run.getScreen().getScreenName() +
                                " ends " + run.getEndDate() + ". Extend it (3/5/7 days) or let it end " +
                                "from the Shows page.");
                run.setLastExtensionPromptForEndDate(tomorrow);
                showRunRepository.save(run);
            }
        } catch (Exception e) {
            log.error("Failed to check for show runs ending soon", e);
        }
    }

    // Runs whose endDate has fully passed: clean up their remaining Show
    // rows (via the same safe deleteShow logic used everywhere else — it
    // already knows how to handle a past show with/without booking
    // history) and mark the run inactive.
    @Scheduled(cron = DAILY_CRON)
    @Transactional
    public void removeEndedRuns() {
        try {
            LocalDate today = LocalDate.now();
            List<ShowRun> ended = showRunRepository.findByActiveTrueAndEndDateBefore(today);

            for (ShowRun run : ended) {
                List<Show> shows = showRepository.findByShowRunId(run.getId());
                for (Show show : shows) {
                    try {
                        showService.deleteShow(show.getId());
                    } catch (Exception e) {
                        // A show with confirmed bookings that somehow hasn't
                        // "ended" by deleteShow's own clock (edge case around
                        // midnight/timezone) gets left for the next run
                        // rather than force-deleting real bookings.
                        log.warn("Couldn't auto-remove show {} from ended run {}: {}",
                                show.getId(), run.getId(), e.getMessage());
                    }
                }
                run.setActive(false);
                showRunRepository.save(run);
            }
        } catch (Exception e) {
            log.error("Failed to clean up ended show runs", e);
        }
    }
}