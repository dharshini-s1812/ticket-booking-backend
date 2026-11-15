package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Response.SeatMapResponse;
import com.TicketBooking.DTO.Response.ShowResponse;
import com.TicketBooking.Service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class PublicShowController {

    private final ShowService showService;

    // e.g. GET /api/shows?movieId=3&location=Coimbatore&showDate=2026-08-10
    @GetMapping
    public ResponseEntity<List<ShowResponse>> searchShows(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long theatreId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(showService.searchShows(movieId, theatreId, showDate, location));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShow(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShow(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<SeatMapResponse> getSeatMap(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getSeatMap(id));
    }
}
