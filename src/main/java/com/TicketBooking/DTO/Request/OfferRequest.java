package com.TicketBooking.DTO.Request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
public class OfferRequest {
    private String offerName;
    private String description;
    private Integer discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
}
