package com.TicketBooking.Repository;

import com.TicketBooking.Model.Booking;
import com.TicketBooking.Model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByBookingTimeDesc(Long userId);

    List<Booking> findByShowId(Long showId);

    // Used by the cleanup scheduler to find abandoned holds — a PENDING
    // booking (seats BLOCKED, no payment yet) older than the hold window.
    List<Booking> findByStatusAndBookingTimeBefore(BookingStatus status, LocalDateTime cutoff);
}