package com.TicketBooking.DTO.Request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExtendScheduleRequest {
    // Number of days to push scheduledEndDate out by. Must be > 0.
    private Integer days;
}
