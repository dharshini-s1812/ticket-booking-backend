package com.TicketBooking.Repository;

import com.TicketBooking.Model.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    // Row-level lock so two concurrent bookings can't both grab the same
    // seat — the second transaction blocks here until the first commits or
    // rolls back, then re-reads the now-current status.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.id in :ids")
    List<ShowSeat> findAllForUpdate(@Param("ids") List<Long> ids);

    List<ShowSeat> findByShowIdAndSeatIdIn(Long showId, List<Long> seatIds);
}
