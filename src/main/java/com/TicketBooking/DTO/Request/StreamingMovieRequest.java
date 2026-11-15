package com.TicketBooking.DTO.Request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
public class StreamingMovieRequest {
    private String movieName;
    private String genre;
    private String language;
    private String certificate;
    private Integer duration;
    private Integer screenNumber;
    private LocalDate releaseDate;
    private String description;

    // Optional. If set, this movie's run auto-expires on this date unless
    // extended (see StreamingMovieController#extendSchedule). Leave blank
    // for a movie that should run indefinitely.
    private LocalDate scheduledEndDate;
}
