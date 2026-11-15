package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.ShowRequest;
import com.TicketBooking.DTO.Response.MessageResponse;
import com.TicketBooking.DTO.Response.ShowResponse;
import com.TicketBooking.Service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Admin CRUD for scheduling shows. Mirrors ScreenController/TheatreController
// conventions (plain JSON, no manual auth check yet — /api/** is permitAll
// for now per SecurityConfig).
@RestController
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    public ResponseEntity<ShowResponse> addShow(@RequestBody ShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showService.addShow(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(@PathVariable Long id, @RequestBody ShowRequest request) {
        return ResponseEntity.ok(showService.updateShow(id, request));
    }

    // Admin-initiated cancellation: refunds any confirmed bookings in
    // full and frees the seats, but doesn't delete the show — see
    // DELETE /{id} for when that becomes possible afterward.
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ShowResponse> cancelShow(@PathVariable Long id) {
        return ResponseEntity.ok(showService.cancelShow(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.ok(new MessageResponse("Show deleted successfully"));
    }

    // Fixes a show stuck at 0 real seats (scheduled while its screen had
    // none) by generating its ShowSeat rows now, from the screen's current
    // seats.
    @PostMapping("/{id}/regenerate-seats")
    public ResponseEntity<ShowResponse> regenerateSeats(@PathVariable Long id) {
        return ResponseEntity.ok(showService.regenerateSeats(id));
    }

    @GetMapping
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShow(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShow(id));
    }
}