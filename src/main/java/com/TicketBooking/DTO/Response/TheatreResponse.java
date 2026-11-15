package com.TicketBooking.DTO.Response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreResponse {

    private Long id;
    private String theatreName;
    private String address;
    private String city;
    private String phone;
}
