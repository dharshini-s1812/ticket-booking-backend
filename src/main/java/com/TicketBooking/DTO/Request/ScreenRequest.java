package com.TicketBooking.DTO.Request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenRequest {

    private String screenName;
    private Integer totalSeats;
    private Long theatreId;
}
