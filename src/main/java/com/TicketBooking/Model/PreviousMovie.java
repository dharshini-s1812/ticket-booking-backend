package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "previous_movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousMovie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String movieName;
    @Column(nullable = false)
    private String genre;
    @Column(nullable = false)
    private String language;
    @Column(nullable = false)
    private String certificate;
    @Column(nullable = false)
    private Integer duration;
    // See StreamingMovie.screenNumber — kept nullable for the same reason.
    private Integer screenNumber;
    @Column(nullable = false)
    private LocalDate releaseDate;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private Boolean active = false;
}