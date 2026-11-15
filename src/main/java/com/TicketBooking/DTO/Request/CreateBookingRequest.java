package com.TicketBooking.DTO.Request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {
    private Long showId;
    // IDs from ShowSeatResponse.showSeatId (not the raw Seat id) — these
    // identify a seat within *this specific show*, which is what actually
    // needs to be locked/held.
    private List<Long> showSeatIds;
}
