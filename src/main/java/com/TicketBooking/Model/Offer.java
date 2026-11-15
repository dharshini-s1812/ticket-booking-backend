package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String offerName;
    @Column(length = 1000)
    private String description;
    private Integer discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active = true;
}
