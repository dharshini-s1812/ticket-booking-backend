package com.TicketBooking.Repository;

import com.TicketBooking.Model.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    long countByTheatreId(Long theatreId);
}
