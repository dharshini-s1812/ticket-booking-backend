package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.StreamingMovieRequest;
import com.TicketBooking.Model.StreamingMovie;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StreamingMovieService {
    void addMovie(StreamingMovieRequest request, MultipartFile image);
    List<StreamingMovie> getAllMovies();
    StreamingMovie getMovieById(Long id);
    void deleteMovie(Long id);
    StreamingMovie updateMovie(Long id,
                               StreamingMovieRequest request,
                               MultipartFile image);
    // location is optional — pass null/blank to get every location
    List<StreamingMovie> getNowShowingMovies(String location);
    List<StreamingMovie> getComingSoonMovies(String location);

    // Pushes a movie's scheduledEndDate out by `days` (from today, or from
    // the current scheduledEndDate if that's still in the future — whichever
    // is later) and clears the "warned" flag so a fresh warning can fire
    // again closer to the new date. days must be > 0.
    StreamingMovie extendSchedule(Long id, int days);

    // Called by MovieExpiryScheduler once a day: warns admins about movies
    // whose scheduledEndDate is coming up soon, and auto-removes (movie +
    // all its shows) any whose scheduledEndDate has already passed without
    // being extended.
    void processScheduledExpiries();
}
