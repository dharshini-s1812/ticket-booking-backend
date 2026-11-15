package com.TicketBooking.DTO.Response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowResponse {
    private Long id;

    private Long movieId;
    private String movieName;
    private String coverImage;
    private Integer duration;
    private String certificate;

    private Long screenId;
    private String screenName;
    private Long theatreId;
    private String theatreName;
    private String city;

    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String language;
    private Double basePrice;

    private Integer availableSeats;
    private Integer totalSeats;

    // True when this show has 0 real ShowSeat rows — usually because it was
    // scheduled while its screen had no seats generated yet. Fixed via
    // POST /api/admin/shows/{id}/regenerate-seats once the screen itself
    // has seats.
    private Boolean misconfigured;
    private Boolean cancelled;

    // Null for a one-off show. Set when this is one day of a multi-day
    // run — the frontend groups the shows table by runId so "Aug 20-27,
    // 9PM" shows as one row instead of 8.
    private Long runId;
    private LocalDate runStartDate;
    private LocalDate runEndDate;
}
