package com.TicketBooking.Repository;

import com.TicketBooking.Model.PreviousMovie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreviousMovieRepository
        extends JpaRepository<PreviousMovie, Long> {
}
