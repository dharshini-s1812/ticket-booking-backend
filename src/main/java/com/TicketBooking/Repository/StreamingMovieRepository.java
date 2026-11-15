package com.TicketBooking.Repository;

import com.TicketBooking.Model.StreamingMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamingMovieRepository extends JpaRepository<StreamingMovie, Long> {
    List<StreamingMovie> findByActiveTrue();

    // Movies with a fixed run window that's still in force — candidates for
    // the daily expiry warning/auto-removal check.
    List<StreamingMovie> findByActiveTrueAndScheduledEndDateIsNotNull();
}
