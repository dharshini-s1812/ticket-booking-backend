package com.TicketBooking.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddAdminResponse {
    private Long id;
    private String email;
}
