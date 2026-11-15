package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.ShowRequest;
import com.TicketBooking.DTO.Response.SeatMapResponse;
import com.TicketBooking.DTO.Response.ShowResponse;
import com.TicketBooking.DTO.Response.ShowRunResponse;
import com.TicketBooking.DTO.Response.ShowSeatResponse;
import com.TicketBooking.Exception.BadRequestException;
import com.TicketBooking.Exception.ConflictException;
import com.TicketBooking.Exception.ResourceNotFoundException;
import com.TicketBooking.Model.*;
import com.TicketBooking.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final StreamingMovieRepository movieRepository;
    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final ShowRunRepository showRunRepository;

    @Override
    @Transactional
    public ShowResponse addShow(ShowRequest request) {

        StreamingMovie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        LocalDate startDate = request.getShowDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : startDate;
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date can't be before the start date.");
        }

        LocalTime endTime = computeEndTime(request.getStartTime(), movie.getDuration());
        List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1)).toList();

        // Reject the whole range up front if ANY day conflicts, rather than
        // creating a partial run and leaving the admin to figure out which
        // days silently didn't get scheduled.
        for (LocalDate date : dates) {
            validateNoOverlap(screen.getId(), date, request.getStartTime(), endTime, null);
        }

        // A single-day request (the common case, and all pre-existing
        // callers) skips the ShowRun wrapper entirely — no behaviour change
        // for one-off shows.
        ShowRun run = null;
        if (dates.size() > 1) {
            run = showRunRepository.save(ShowRun.builder()
                    .movie(movie)
                    .screen(screen)
                    .startDate(startDate)
                    .endDate(endDate)
                    .startTime(request.getStartTime())
                    .language(request.getLanguage())
                    .basePrice(request.getBasePrice())
                    .build());
        }

        Show firstShow = null;
        for (LocalDate date : dates) {
            Show show = Show.builder()
                    .movie(movie)
                    .screen(screen)
                    .showRun(run)
                    .showDate(date)
                    .startTime(request.getStartTime())
                    .endTime(endTime)
                    .language(request.getLanguage())
                    .basePrice(request.getBasePrice())
                    .build();
            show = showRepository.save(show);
            generateShowSeats(show, screen);
            if (firstShow == null) firstShow = show;
        }

        return mapToResponse(firstShow);
    }

    @Override
    @Transactional
    public ShowResponse updateShow(Long id, ShowRequest request) {

        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        // If any seat has already been blocked/booked, changing the
        // schedule out from under those bookings isn't safe — reject it.
        boolean hasActivity = showSeatRepository.findByShowId(id).stream()
                .anyMatch(ss -> ss.getStatus() != SeatStatus.AVAILABLE);
        if (hasActivity) {
            throw new ConflictException("This show already has bookings/holds and can't be rescheduled. Cancel it and create a new one instead.");
        }

        StreamingMovie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        LocalTime endTime = computeEndTime(request.getStartTime(), movie.getDuration());

        validateNoOverlap(screen.getId(), request.getShowDate(), request.getStartTime(), endTime, id);

        boolean screenChanged = !screen.getId().equals(show.getScreen().getId());

        show.setMovie(movie);
        show.setScreen(screen);
        show.setShowDate(request.getShowDate());
        show.setStartTime(request.getStartTime());
        show.setEndTime(endTime);
        show.setLanguage(request.getLanguage());
        show.setBasePrice(request.getBasePrice());

        show = showRepository.save(show);

        // Screen changed -> seat layout is for a different room. Regenerate.
        if (screenChanged) {
            showSeatRepository.deleteAll(showSeatRepository.findByShowId(id));
            generateShowSeats(show, screen);
        }

        return mapToResponse(show);
    }

    @Override
    @Transactional
    public void deleteShow(Long id) {

        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        List<Booking> bookings = bookingRepository.findByShowId(id);
        List<Booking> confirmed = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();

        boolean showHasEnded = LocalDateTime.of(show.getShowDate(), show.getEndTime())
                .isBefore(LocalDateTime.now());

        if (!confirmed.isEmpty() && !showHasEnded) {
            // Real, paid tickets exist and the show hasn't happened yet —
            // don't silently wipe them out. Cancel first (mandatory refund
            // via cancelShow), which frees the seats, then delete.
            throw new ConflictException(
                    "This show has " + confirmed.size() + " confirmed booking(s) and hasn't happened yet. " +
                            "Cancel the show first (this refunds those bookings in full), then delete it.");
        }

        // Past its scheduled end, OR only ever had abandoned/cancelled
        // holds (no confirmed booking) — safe to purge. Note: for a
        // *past* show with confirmed bookings, this does delete that
        // booking/payment history along with it (simple first pass, as
        // discussed — worth revisiting later if you want to keep a
        // permanent record of completed, paid shows instead).
        for (Booking booking : bookings) {
            bookedSeatRepository.deleteAll(bookedSeatRepository.findByBookingId(booking.getId()));
            paymentRepository.findByBookingId(booking.getId()).ifPresent(paymentRepository::delete);
        }
        bookingRepository.deleteAll(bookings);

        showSeatRepository.deleteAll(showSeatRepository.findByShowId(id));
        ShowRun run = show.getShowRun();
        showRepository.delete(show);

        // If this was the last day of a run, the run itself is done too.
        if (run != null && showRepository.findByShowRunId(run.getId()).isEmpty()) {
            run.setActive(false);
            showRunRepository.save(run);
        }
    }

    @Override
    @Transactional
    public ShowResponse cancelShow(Long id) {

        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        if (show.getCancelled()) {
            throw new ConflictException("This show is already cancelled.");
        }

        List<Booking> bookings = bookingRepository.findByShowId(id).stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .toList();

        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                // Admin-initiated cancellation — unlike a customer
                // cancelling their own booking, the customer didn't choose
                // this, so the refund is the full amount, no fee.
                paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
                    payment.setPaymentStatus(PaymentStatus.REFUNDED);
                    payment.setRefundAmount(payment.getAmount());
                    paymentRepository.save(payment);
                });
                notificationService.notifyUser(
                        booking.getUser(),
                        NotificationType.SHOW_CANCELLED,
                        "Show cancelled",
                        "The show for \"" + show.getMovie().getMovieName() + "\" on " + show.getShowDate() +
                                " at " + show.getStartTime() + " has been cancelled. You've been refunded in full.");
            }
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }

        List<ShowSeat> showSeats = showSeatRepository.findByShowId(id);
        showSeats.forEach(ss -> ss.setStatus(SeatStatus.AVAILABLE));
        showSeatRepository.saveAll(showSeats);

        show.setCancelled(true);
        showRepository.save(show);

        return mapToResponse(show);
    }

    @Override
    public List<ShowRunResponse> getRunsEndingSoon() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return showRunRepository.findByActiveTrueAndEndDate(tomorrow).stream()
                .map(this::mapRunToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ShowRunResponse extendRun(Long runId, int days) {
        ShowRun run = showRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Run not found"));

        if (!run.getActive()) {
            throw new ConflictException("This run has already ended and can't be extended.");
        }

        LocalDate oldEnd = run.getEndDate();
        LocalDate newEnd = oldEnd.plusDays(days);
        LocalTime endTime = computeEndTime(run.getStartTime(), run.getMovie().getDuration());

        List<LocalDate> newDates = oldEnd.plusDays(1).datesUntil(newEnd.plusDays(1)).toList();
        for (LocalDate date : newDates) {
            validateNoOverlap(run.getScreen().getId(), date, run.getStartTime(), endTime, null);
        }

        for (LocalDate date : newDates) {
            Show show = Show.builder()
                    .movie(run.getMovie())
                    .screen(run.getScreen())
                    .showRun(run)
                    .showDate(date)
                    .startTime(run.getStartTime())
                    .endTime(endTime)
                    .language(run.getLanguage())
                    .basePrice(run.getBasePrice())
                    .build();
            show = showRepository.save(show);
            generateShowSeats(show, run.getScreen());
        }

        run.setEndDate(newEnd);
        // Reset so the "ending soon" job prompts again for the new endDate
        // instead of treating this as already-handled.
        run.setLastExtensionPromptForEndDate(null);
        run = showRunRepository.save(run);

        return mapRunToResponse(run);
    }

    @Override
    @Transactional
    public void declineExtension(Long runId) {
        ShowRun run = showRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Run not found"));
        run.setLastExtensionPromptForEndDate(run.getEndDate());
        showRunRepository.save(run);
    }

    @Override
    @Transactional
    public void cancelRun(Long runId) {
        ShowRun run = showRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Run not found"));

        List<Show> shows = showRepository.findByShowRunId(runId);
        for (Show show : shows) {
            if (!show.getCancelled()) {
                cancelShow(show.getId());
            }
        }

        run.setActive(false);
        showRunRepository.save(run);
    }

    private ShowRunResponse mapRunToResponse(ShowRun run) {
        int showCount = showRepository.findByShowRunId(run.getId()).size();
        return ShowRunResponse.builder()
                .id(run.getId())
                .movieId(run.getMovie().getId())
                .movieName(run.getMovie().getMovieName())
                .screenId(run.getScreen().getId())
                .screenName(run.getScreen().getScreenName())
                .theatreName(run.getScreen().getTheatre().getTheatreName())
                .city(run.getScreen().getTheatre().getCity())
                .startDate(run.getStartDate())
                .endDate(run.getEndDate())
                .startTime(run.getStartTime())
                .basePrice(run.getBasePrice())
                .active(run.getActive())
                .showCount(showCount)
                .build();
    }

    @Override
    public ShowResponse getShow(Long id) {
        return mapToResponse(showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found")));
    }

    @Override
    public List<ShowResponse> getAllShows() {
        return showRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<ShowResponse> searchShows(Long movieId, Long theatreId, LocalDate showDate, String location) {
        String loc = (location == null || location.isBlank()) ? null : location;
        return showRepository.search(movieId, theatreId, showDate, loc)
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getCancelled()))
                .map(this::mapToResponse).toList();
    }

    @Override
    public SeatMapResponse getSeatMap(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        List<ShowSeatResponse> seats = showSeatRepository.findByShowId(showId).stream()
                .map(ss -> ShowSeatResponse.builder()
                        .showSeatId(ss.getId())
                        .seatId(ss.getSeat().getId())
                        .seatNumber(ss.getSeat().getSeatNumber())
                        .seatType(ss.getSeat().getSeatType())
                        .price(show.getBasePrice() + ss.getSeat().getPrice())
                        .status(ss.getStatus())
                        .build())
                .sorted((a, b) -> a.getSeatNumber().compareTo(b.getSeatNumber()))
                .toList();

        return SeatMapResponse.builder()
                .showId(show.getId())
                .movieName(show.getMovie().getMovieName())
                .theatreName(show.getScreen().getTheatre().getTheatreName())
                .screenName(show.getScreen().getScreenName())
                .seats(seats)
                .build();
    }

    @Override
    @Transactional
    public ShowResponse regenerateSeats(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        long existing = showSeatRepository.findByShowId(showId).size();
        if (existing > 0) {
            throw new ConflictException(
                    "This show already has " + existing + " seat(s) generated — nothing to fix.");
        }

        long screenSeats = seatRepository.countByScreenId(show.getScreen().getId());
        if (screenSeats == 0) {
            throw new ConflictException(
                    "This show's screen has no seats generated yet. Fix the screen itself first " +
                            "(Theatres page → that screen → \"Fix seats\"), then retry.");
        }

        generateShowSeats(show, show.getScreen());

        return mapToResponse(show);
    }

    // ---- helpers ----

    private void generateShowSeats(Show show, Screen screen) {
        List<Seat> seats = seatRepository.findByScreenId(screen.getId());
        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> ShowSeat.builder()
                        .show(show)
                        .seat(seat)
                        .status(SeatStatus.AVAILABLE)
                        .build())
                .toList();
        showSeatRepository.saveAll(showSeats);
    }

    private LocalTime computeEndTime(LocalTime startTime, Integer durationMinutes) {
        // + 15 min buffer for cleaning/trailers between shows.
        return startTime.plusMinutes(durationMinutes + 15L);
    }

    private void validateNoOverlap(Long screenId, LocalDate showDate, LocalTime start, LocalTime end, Long excludeShowId) {
        List<Show> sameDay = showRepository.findByScreenIdAndShowDate(screenId, showDate);
        for (Show existing : sameDay) {
            if (excludeShowId != null && existing.getId().equals(excludeShowId)) continue;
            boolean overlaps = start.isBefore(existing.getEndTime()) && existing.getStartTime().isBefore(end);
            if (overlaps) {
                throw new ConflictException(
                        "This screen already has a show from " + existing.getStartTime() +
                                " to " + existing.getEndTime() + " on " + showDate);
            }
        }
    }

    private ShowResponse mapToResponse(Show show) {
        // IMPORTANT: totalSeats must reflect THIS show's own ShowSeat rows,
        // not the screen's current Seat count. Those two can legitimately
        // diverge — e.g. a show scheduled while its screen had 0 seats
        // keeps 0 ShowSeat rows forever, even after the screen is later
        // fixed, until this show is regenerated too (see regenerateSeats).
        // Counting the screen instead was the original bug: the shows
        // table showed "100/100 available" for a show whose seat map was
        // actually empty.
        List<ShowSeat> showSeats = showSeatRepository.findByShowId(show.getId());
        long totalSeats = showSeats.size();
        long taken = showSeats.stream().filter(ss -> ss.getStatus() != SeatStatus.AVAILABLE).count();

        return ShowResponse.builder()
                .id(show.getId())
                .movieId(show.getMovie().getId())
                .movieName(show.getMovie().getMovieName())
                .coverImage(show.getMovie().getCoverImage())
                .duration(show.getMovie().getDuration())
                .certificate(show.getMovie().getCertificate())
                .screenId(show.getScreen().getId())
                .screenName(show.getScreen().getScreenName())
                .theatreId(show.getScreen().getTheatre().getId())
                .theatreName(show.getScreen().getTheatre().getTheatreName())
                .city(show.getScreen().getTheatre().getCity())
                .showDate(show.getShowDate())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .language(show.getLanguage())
                .basePrice(show.getBasePrice())
                .totalSeats((int) totalSeats)
                .availableSeats((int) (totalSeats - taken))
                .misconfigured(totalSeats == 0)
                .cancelled(show.getCancelled())
                .runId(show.getShowRun() != null ? show.getShowRun().getId() : null)
                .runStartDate(show.getShowRun() != null ? show.getShowRun().getStartDate() : null)
                .runEndDate(show.getShowRun() != null ? show.getShowRun().getEndDate() : null)
                .build();
    }
}
