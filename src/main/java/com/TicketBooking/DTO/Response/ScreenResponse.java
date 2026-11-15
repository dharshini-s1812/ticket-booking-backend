package com.TicketBooking.DTO.Response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenResponse {

    private Long id;
    private String screenName;
    private Integer totalSeats;

    // Actual number of Seat rows generated for this screen. Normally equal
    // to totalSeats. Exposed separately so the frontend can flag screens
    // that were created without their seat grid ever being generated
    // (e.g. inserted directly into the DB, bypassing addScreen()).
    private Long actualSeatCount;
    private Boolean misconfigured;

    private Long theatreId;
    private String theatreName;
}
