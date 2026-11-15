package com.TicketBooking.DTO.Request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    // Simulated gateway for now — card / upi / netbanking, purely descriptive.
    private String paymentMethod;
}
