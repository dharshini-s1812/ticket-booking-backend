package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.ShowRequest;
import com.TicketBooking.DTO.Response.SeatMapResponse;
import com.TicketBooking.DTO.Response.ShowResponse;
import com.TicketBooking.DTO.Response.ShowRunResponse;

import java.time.LocalDate;
import java.util.List;

public interface ShowService {

    ShowResponse addShow(ShowRequest request);

    ShowResponse updateShow(Long id, ShowRequest request);

    void deleteShow(Long id);

    ShowResponse getShow(Long id);

    List<ShowResponse> getAllShows();

    // Public search — any filter may be null/blank to skip it.
    List<ShowResponse> searchShows(Long movieId, Long theatreId, LocalDate showDate, String location);

    SeatMapResponse getSeatMap(Long showId);

    // Fixes a show that has 0 real ShowSeat rows (misconfigured) by
    // generating them from the screen's current seats. Only allowed while
    // the show truly has 0 ShowSeat rows.
    ShowResponse regenerateSeats(Long showId);

    // Admin-initiated cancellation: refunds any confirmed bookings in full,
    // releases all holds/seats, and marks the show cancelled so it can
    // never be booked again. Doesn't delete the show — see deleteShow for
    // when that's actually possible.
    ShowResponse cancelShow(Long showId);

    // Runs (see ShowRun) whose current endDate is tomorrow and haven't
    // already been prompted about — what the "extend or let it end" panel
    // shows the admin.
    List<ShowRunResponse> getRunsEndingSoon();

    // Pushes a run's endDate out by `days` and creates the newly-added
    // Show rows for the extra dates.
    ShowRunResponse extendRun(Long runId, int days);

    // Admin explicitly chose not to extend — just stops prompting about
    // this run's current endDate; it still ends and gets cleaned up
    // naturally on schedule.
    void declineExtension(Long runId);

    // Cancels every remaining show under a run (mandatory refund for any
    // confirmed bookings — same semantics as cancelShow) and marks the run
    // inactive.
    void cancelRun(Long runId);
}
