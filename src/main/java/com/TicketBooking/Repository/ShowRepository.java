package com.TicketBooking.Repository;

import com.TicketBooking.Model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByScreenIdAndShowDate(Long screenId, LocalDate showDate);

    List<Show> findByMovieId(Long movieId);

    // Public listing filters — any combination of movieId/theatreId/date can
    // be null, in which case that filter is skipped.
    @Query("select s from Show s " +
           "where (:movieId is null or s.movie.id = :movieId) " +
           "and (:theatreId is null or s.screen.theatre.id = :theatreId) " +
           "and (:showDate is null or s.showDate = :showDate) " +
           "and (:location is null or lower(s.screen.theatre.city) = lower(:location)) " +
           "order by s.showDate asc, s.startTime asc")
    List<Show> search(@Param("movieId") Long movieId,
                       @Param("theatreId") Long theatreId,
                       @Param("showDate") LocalDate showDate,
                       @Param("location") String location);

    List<Show> findByScreenId(Long screenId);

    List<Show> findByShowRunId(Long showRunId);

    // "Now showing in <city>" is derived from real scheduling, not a
    // movie-level location field — a movie counts as showing in a city if
    // it has at least one Show on a screen belonging to a theatre there.
    @Query("select distinct s.movie.id from Show s where lower(s.screen.theatre.city) = lower(:city)")
    List<Long> findMovieIdsByCity(@Param("city") String city);

    // Same idea, city-agnostic: any movie with at least one Show anywhere is
    // "Now Showing" rather than "Coming Soon" — see StreamingMovieServiceImpl.
    @Query("select distinct s.movie.id from Show s")
    List<Long> findAllScheduledMovieIds();
}
