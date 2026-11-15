package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.ScreenRequest;
import com.TicketBooking.DTO.Response.MessageResponse;
import com.TicketBooking.DTO.Response.ScreenResponse;
import com.TicketBooking.Service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    @PostMapping
    public ResponseEntity<ScreenResponse> addScreen(@RequestBody ScreenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(screenService.addScreen(request));
    }

    @GetMapping
    public ResponseEntity<List<ScreenResponse>> getAllScreens() {
        return ResponseEntity.ok(screenService.getAllScreens());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreenResponse> getScreen(@PathVariable Long id) {
        return ResponseEntity.ok(screenService.getScreen(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScreenResponse> updateScreen(@PathVariable Long id,
                                                       @RequestBody ScreenRequest request) {
        return ResponseEntity.ok(screenService.updateScreen(id, request));
    }

    // Fixes a screen stuck at 0 real seats (e.g. created outside the normal
    // addScreen() flow) by generating its seat grid now.
    @PostMapping("/{id}/regenerate-seats")
    public ResponseEntity<ScreenResponse> regenerateSeats(@PathVariable Long id) {
        return ResponseEntity.ok(screenService.regenerateSeats(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteScreen(@PathVariable Long id) {

        screenService.deleteScreen(id);

        return ResponseEntity.ok(new MessageResponse("Screen deleted successfully"));
    }
}
