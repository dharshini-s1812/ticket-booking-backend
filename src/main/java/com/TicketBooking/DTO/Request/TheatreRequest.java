package com.TicketBooking.DTO.Request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreRequest {

    private String theatreName;
    private String address;
    private String city;
    private String phone;
}
