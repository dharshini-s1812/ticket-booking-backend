package com.TicketBooking.DTO.Response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatMapResponse {
    private Long showId;
    private String movieName;
    private String theatreName;
    private String screenName;
    private List<ShowSeatResponse> seats;
}
