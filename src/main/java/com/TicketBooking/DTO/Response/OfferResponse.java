package com.TicketBooking.DTO.Response;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class OfferResponse {
    private Long id;
    private String offerName;
    private String description;
    private Integer discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
}
