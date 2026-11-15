package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.TheatreRequest;
import com.TicketBooking.DTO.Response.MessageResponse;
import com.TicketBooking.DTO.Response.TheatreResponse;
import com.TicketBooking.Service.TheatreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
public class TheatreController {

    private final TheatreService theatreService;

    @PostMapping
    public ResponseEntity<TheatreResponse> addTheatre(
            @RequestBody TheatreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(theatreService.addTheatre(request));
    }

    @GetMapping
    public ResponseEntity<List<TheatreResponse>> getAllTheatres() {
        return ResponseEntity.ok(theatreService.getAllTheatres());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TheatreResponse> getTheatreById(
            @PathVariable Long id) {
        return ResponseEntity.ok(theatreService.getTheatre(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TheatreResponse> updateTheatre(
            @PathVariable Long id,
            @RequestBody TheatreRequest request) {
        return ResponseEntity.ok(theatreService.updateTheatre(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTheatre(
            @PathVariable Long id) {

        theatreService.deleteTheatre(id);

        return ResponseEntity.ok(
                new MessageResponse("Theatre deleted successfully")
        );
    }
}
