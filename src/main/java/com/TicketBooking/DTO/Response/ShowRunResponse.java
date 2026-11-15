package com.TicketBooking.DTO.Response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ShowRunResponse {
    private Long id;
    private Long movieId;
    private String movieName;
    private Long screenId;
    private String screenName;
    private String theatreName;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private Double basePrice;
    private Boolean active;
    private Integer showCount;
}
