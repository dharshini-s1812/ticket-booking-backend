package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.ExtendScheduleRequest;
import com.TicketBooking.DTO.Request.StreamingMovieRequest;
import com.TicketBooking.Model.StreamingMovie;
import com.TicketBooking.Service.StreamingMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/streaming-movies")
@RequiredArgsConstructor
public class StreamingMovieController {

    private final StreamingMovieService streamingMovieService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> addMovie(
            @ModelAttribute StreamingMovieRequest request,
            @RequestPart("image") MultipartFile image
    ) {
        streamingMovieService.addMovie(request, image);
        return ResponseEntity.ok("Movie added successfully.");
    }

    @GetMapping
    public ResponseEntity<List<StreamingMovie>> getAllMovies() {
        return ResponseEntity.ok(streamingMovieService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StreamingMovie> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(streamingMovieService.getMovieById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMovie(@PathVariable Long id) {
        streamingMovieService.deleteMovie(id);
        return ResponseEntity.ok("Movie deleted successfully.");
    }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StreamingMovie> updateMovie(
            @PathVariable Long id,
            @ModelAttribute StreamingMovieRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return ResponseEntity.ok(
                streamingMovieService.updateMovie(id, request, image)
        );
    }

    // Pushes a movie's scheduled end date out by N days — the admin's
    // response to a "run ending soon" notification when they don't want it
    // (and its shows) to be auto-removed.
    @PatchMapping("/{id}/extend")
    public ResponseEntity<StreamingMovie> extendSchedule(
            @PathVariable Long id,
            @RequestBody ExtendScheduleRequest request
    ) {
        int days = request.getDays() == null ? 0 : request.getDays();
        return ResponseEntity.ok(streamingMovieService.extendSchedule(id, days));
    }
}
