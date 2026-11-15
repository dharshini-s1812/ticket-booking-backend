package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    private String transactionId;

    @Column(nullable = false)
    private LocalDateTime paymentTime;

    // Populated only when paymentStatus is REFUNDED. Can be less than
    // `amount` — a customer-initiated cancellation deducts a cancellation
    // fee (see BookingServiceImpl.CANCELLATION_FEE_PERCENT), while an
    // admin-initiated show cancellation always refunds the full amount.
    private Double refundAmount;
}