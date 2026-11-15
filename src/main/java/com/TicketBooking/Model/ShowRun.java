package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

// A "run" is a movie scheduled on one screen across a range of dates (same
// time and price every day). Each individual day still gets its own Show
// row (bookings/seats attach there, unchanged) — this just groups them so
// the admin can manage "Aug 20-27, 9PM" as one thing instead of 8 separate
// rows, and so the extend-or-end reminder has something to track.
@Entity
@Table(name = "show_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private StreamingMovie movie;

    @ManyToOne
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private Double basePrice;

    // False once the run has ended and its shows were cleaned up, or the
    // admin cancelled it outright.
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    // Which endDate we last sent an "ending soon, extend?" notification
    // for. Compared against the current endDate so extending resets it
    // (a new reminder fires for the new endDate) and so a run whose
    // endDate hasn't changed doesn't get reminded every day it's ignored.
    private LocalDate lastExtensionPromptForEndDate;
}
