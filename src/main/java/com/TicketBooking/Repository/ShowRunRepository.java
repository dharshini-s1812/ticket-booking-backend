package com.TicketBooking.Repository;

import com.TicketBooking.Model.ShowRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ShowRunRepository extends JpaRepository<ShowRun, Long> {

    // "Ending soon" candidates: still active, whose current endDate we
    // haven't already prompted about.
    List<ShowRun> findByActiveTrueAndEndDate(LocalDate endDate);

    // Fully over — safe to clean up.
    List<ShowRun> findByActiveTrueAndEndDateBefore(LocalDate date);

    List<ShowRun> findByScreenId(Long screenId);
}
