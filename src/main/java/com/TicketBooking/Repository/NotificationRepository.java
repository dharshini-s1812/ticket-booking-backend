package com.TicketBooking.Repository;

import com.TicketBooking.Model.Notification;
import com.TicketBooking.Model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    // Used to throttle chatty notification types (LOGIN, NEW_MOVIE) — only
    // send another one if the last one of that type is older than the
    // caller's cutoff.
    boolean existsByUserIdAndTypeAndCreatedAtAfter(Long userId, NotificationType type, LocalDateTime after);
}
