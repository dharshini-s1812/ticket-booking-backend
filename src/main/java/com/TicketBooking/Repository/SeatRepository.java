package com.TicketBooking.Repository;

import com.TicketBooking.Model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreenId(Long screenId);
    void deleteByScreenId(Long screenId);
    long countByScreenId(Long screenId);
}
