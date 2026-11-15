package com.TicketBooking.Repository;

import com.TicketBooking.Model.RefreshToken;
import com.TicketBooking.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // @Modifying makes this a bulk JPQL DELETE that runs immediately via
    // executeUpdate(), instead of Spring Data's default derived-delete
    // behavior (select matching rows, then queue entity removals for the
    // next flush). Without this, Hibernate's flush ordering runs pending
    // INSERTs before pending DELETEs, so a new refresh token insert for the
    // same user can hit the unique constraint on user_id before the old
    // row is actually removed.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshToken rt where rt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
