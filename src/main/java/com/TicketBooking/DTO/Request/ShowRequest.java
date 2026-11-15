package com.TicketBooking.DTO.Request;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowRequest {
    private Long movieId;
    private Long screenId;
    private LocalDate showDate;
    // Optional. Null or equal to showDate = a single one-off show (existing
    // behaviour, unchanged). Anything later creates one Show per day in
    // [showDate, endDate] under a shared ShowRun — see ShowServiceImpl.
    private LocalDate endDate;
    private LocalTime startTime;
    private String language;
    private Double basePrice;
}
