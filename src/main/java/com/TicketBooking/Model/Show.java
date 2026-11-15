package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private StreamingMovie movie;

    @ManyToOne
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    // Null for a one-off show created without a date range. Set when this
    // Show is one day of a multi-day ShowRun — see ShowRun for why this
    // exists instead of Show itself having a date range.
    @ManyToOne
    @JoinColumn(name = "show_run_id")
    private ShowRun showRun;

    @Column(nullable = false)
    private LocalDate showDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private Double basePrice;

    // Set by an admin "Cancel show" action (mandatory full refund for any
    // confirmed bookings). A cancelled show is never bookable again, even
    // though its ShowSeat rows go back to AVAILABLE when cancelled — see
    // ShowServiceImpl.cancelShow / BookingServiceImpl.createBooking.
    @Builder.Default
    @Column(nullable = false)
    private Boolean cancelled = false;
}
