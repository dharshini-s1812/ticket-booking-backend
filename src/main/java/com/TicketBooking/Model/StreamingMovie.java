package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "streaming_movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamingMovie {

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
    private Integer screenNumber;
    @Column(nullable = false)
    private LocalDate releaseDate;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private String coverImage;
    @Column(nullable = false)
    private Boolean active = true;

    // Optional fixed-period run: if set, this movie (and every Show still
    // attached to it) is automatically removed on this date unless an admin
    // extends it first. Null = runs indefinitely, same behaviour as before
    // this feature existed. See MovieExpiryScheduler / StreamingMovieServiceImpl.
    private LocalDate scheduledEndDate;

    // Whether the "your movie's run is ending soon" admin notification has
    // already gone out for the CURRENT scheduledEndDate, so the daily
    // scheduler doesn't re-notify every single day. Reset to false whenever
    // scheduledEndDate is set/changed (new schedule or an extension).
    // columnDefinition (not just nullable=false) matters here: on an
    // existing table, Hibernate's "add column ... not null" with no
    // DEFAULT fails against Postgres because current rows have nothing to
    // put in the new NOT NULL column. Adding "default false" to the DDL
    // lets Postgres backfill existing rows automatically.
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean expiryWarningSent = false;

    // Not persisted. "Now Showing" vs "Coming Soon" is derived from whether
    // this movie actually has any Show scheduled (see StreamingMovieServiceImpl)
    // rather than a manually-picked field — same reasoning as dropping the
    // old manual "location" field. Populated by the service layer on read.
    @Transient
    private MovieStatus status;
}
