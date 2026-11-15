package com.TicketBooking.Repository;

import com.TicketBooking.Model.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    boolean existsByTheatreName(String theatreName);
}
