package com.TicketBooking.Controller;

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
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final StreamingMovieService streamingMovieService;

    @GetMapping("/now-showing")
    public ResponseEntity<List<StreamingMovie>> getNowShowingMovies(
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(streamingMovieService.getNowShowingMovies(location));
    }

    @GetMapping("/coming-soon")
    public ResponseEntity<List<StreamingMovie>> getComingSoonMovies(
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(streamingMovieService.getComingSoonMovies(location));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StreamingMovie> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(streamingMovieService.getMovieById(id));
    }


}
