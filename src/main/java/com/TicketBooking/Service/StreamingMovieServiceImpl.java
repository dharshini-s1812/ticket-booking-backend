package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.StreamingMovieRequest;
import com.TicketBooking.Exception.ConflictException;
import com.TicketBooking.Exception.ResourceNotFoundException;
import com.TicketBooking.Model.MovieStatus;
import com.TicketBooking.Model.NotificationType;
import com.TicketBooking.Model.PreviousMovie;
import com.TicketBooking.Model.Show;
import com.TicketBooking.Model.StreamingMovie;
import com.TicketBooking.Repository.PreviousMovieRepository;
import com.TicketBooking.Repository.ShowRepository;
import com.TicketBooking.Repository.StreamingMovieRepository;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloudinary.Cloudinary;

@RequiredArgsConstructor
@Service
@Slf4j
public class StreamingMovieServiceImpl implements StreamingMovieService {

    private final StreamingMovieRepository streamingMovieRepository;
    private final Cloudinary cloudinary;
    private final PreviousMovieRepository previousMovieRepository;
    private final ShowRepository showRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ShowService showService;

    // How many days before scheduledEndDate admins get the "ending soon"
    // warning, giving them time to extend before the auto-removal runs.
    private static final int EXPIRY_WARNING_DAYS = 2;

    @Override
    public void addMovie(StreamingMovieRequest request, MultipartFile image) {

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    image.getBytes(),
                    ObjectUtils.emptyMap()
            );
            String imageUrl = uploadResult.get("secure_url").toString();
            StreamingMovie movie = StreamingMovie.builder()
                    .movieName(request.getMovieName())
                    .genre(request.getGenre())
                    .language(request.getLanguage())
                    .certificate(request.getCertificate())
                    .duration(request.getDuration())
                    .screenNumber(request.getScreenNumber())
                    .releaseDate(request.getReleaseDate())
                    .description(request.getDescription())
                    .coverImage(imageUrl)
                    .active(true)
                    .scheduledEndDate(request.getScheduledEndDate())
                    .expiryWarningSent(false)
                    .build();
            streamingMovieRepository.save(movie);

            auditLogService.log(
                    "ADD",
                    "MOVIE",
                    movie.getMovieName(),
                    "Added movie '" + movie.getMovieName() + "'"
                            + (movie.getScheduledEndDate() != null
                                    ? " with scheduled run until " + movie.getScheduledEndDate() + "."
                                    : "."));

            // Fans out once per movie added (not on every edit) — naturally
            // infrequent, which is exactly the "occasionally" cadence asked
            // for, no extra throttling needed on top.
            notificationService.notifyAllUsers(
                    NotificationType.NEW_MOVIE,
                    "New movie added",
                    "\"" + movie.getMovieName() + "\" has just been added — check it out!");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image.", e);
        }
    }

    @Override
    public List<StreamingMovie> getAllMovies() {
        List<StreamingMovie> movies = streamingMovieRepository.findAll();
        Set<Long> scheduledIds = Set.copyOf(showRepository.findAllScheduledMovieIds());
        movies.forEach(m -> m.setStatus(
                scheduledIds.contains(m.getId()) ? MovieStatus.NOW_SHOWING : MovieStatus.COMING_SOON));
        return movies;
    }

    @Override
    public StreamingMovie getMovieById(Long id) {
        StreamingMovie movie = streamingMovieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        boolean hasShows = !showRepository.findByMovieId(id).isEmpty();
        movie.setStatus(hasShows ? MovieStatus.NOW_SHOWING : MovieStatus.COMING_SOON);
        return movie;
    }

    @Override
    public void deleteMovie(Long id) {

        StreamingMovie movie = streamingMovieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        // Show.movie_id is a hard FK with no cascade — deleting a movie
        // that still has shows scheduled would otherwise hit a raw DB
        // constraint violation (surfaced to the admin as an unhelpful
        // generic 500). Block it with a clear message instead.
        int showCount = showRepository.findByMovieId(id).size();
        if (showCount > 0) {
            throw new ConflictException(
                    "Can't delete \"" + movie.getMovieName() + "\": it has " + showCount +
                            " show(s) scheduled. Remove those on the Shows page first, then delete the movie.");
        }

        PreviousMovie previousMovie = PreviousMovie.builder()
                .movieName(movie.getMovieName())
                .genre(movie.getGenre())
                .language(movie.getLanguage())
                .certificate(movie.getCertificate())
                .duration(movie.getDuration())
                .screenNumber(movie.getScreenNumber())
                .releaseDate(movie.getReleaseDate())
                .description(movie.getDescription())
                .active(false)
                .build();

        previousMovieRepository.save(previousMovie);

        streamingMovieRepository.delete(movie);

        auditLogService.log(
                "DELETE",
                "MOVIE",
                movie.getMovieName(),
                "Removed movie '" + movie.getMovieName() + "'.");
    }
    @Override
    public StreamingMovie updateMovie(Long id,
                                      StreamingMovieRequest request,
                                      MultipartFile image) {

        StreamingMovie movie = streamingMovieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        movie.setMovieName(request.getMovieName());
        movie.setGenre(request.getGenre());
        movie.setLanguage(request.getLanguage());
        movie.setCertificate(request.getCertificate());
        movie.setDuration(request.getDuration());
        movie.setScreenNumber(request.getScreenNumber());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setDescription(request.getDescription());

        // Changing the run's end date (including clearing it, or setting it
        // for the first time) means any previous "ending soon" warning no
        // longer applies to the new date — let it fire again on its own
        // schedule instead of staying silenced.
        boolean scheduleChanged = !java.util.Objects.equals(movie.getScheduledEndDate(), request.getScheduledEndDate());
        movie.setScheduledEndDate(request.getScheduledEndDate());
        if (scheduleChanged) {
            movie.setExpiryWarningSent(false);
        }

        if (image != null && !image.isEmpty()) {
            try {
                Map uploadResult = cloudinary.uploader().upload(
                        image.getBytes(),
                        ObjectUtils.emptyMap()
                );

                String imageUrl = uploadResult.get("secure_url").toString();
                movie.setCoverImage(imageUrl);

            } catch (IOException e) {
                throw new RuntimeException("Failed to upload image.", e);
            }
        }

        StreamingMovie saved = streamingMovieRepository.save(movie);

        auditLogService.log(
                "UPDATE",
                "MOVIE",
                saved.getMovieName(),
                "Updated movie '" + saved.getMovieName() + "'"
                        + (saved.getScheduledEndDate() != null
                                ? " (scheduled run until " + saved.getScheduledEndDate() + ")."
                                : "."));

        return saved;
    }
    @Override
    public List<StreamingMovie> getNowShowingMovies(String location) {
        List<StreamingMovie> active = streamingMovieRepository.findByActiveTrue();

        // A movie counts as "showing" only if it actually has a Show
        // scheduled — and "showing in <city>" only if that Show is on a
        // screen belonging to a theatre there. Not a manually-typed field on
        // the movie itself, which drifts out of sync with real scheduling.
        Set<Long> scheduledIds = (location == null || location.isBlank())
                ? Set.copyOf(showRepository.findAllScheduledMovieIds())
                : Set.copyOf(showRepository.findMovieIdsByCity(location));

        return active.stream()
                .filter(m -> scheduledIds.contains(m.getId()))
                .peek(m -> m.setStatus(MovieStatus.NOW_SHOWING))
                .toList();
    }

    @Override
    public List<StreamingMovie> getComingSoonMovies(String location) {
        // Coming-soon = active movies with NO Show scheduled anywhere yet.
        // There's nothing to derive a city from until one gets scheduled, so
        // city filtering intentionally doesn't apply here (location is
        // accepted but unused) — same as before.
        List<StreamingMovie> active = streamingMovieRepository.findByActiveTrue();
        Set<Long> scheduledIds = Set.copyOf(showRepository.findAllScheduledMovieIds());

        return active.stream()
                .filter(m -> !scheduledIds.contains(m.getId()))
                .peek(m -> m.setStatus(MovieStatus.COMING_SOON))
                .toList();
    }

    @Override
    @Transactional
    public StreamingMovie extendSchedule(Long id, int days) {

        if (days <= 0) {
            throw new ConflictException("Extension must be at least 1 day.");
        }

        StreamingMovie movie = streamingMovieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        LocalDate today = LocalDate.now();
        // Extend from whichever is later: today, or the current end date
        // (covers both "still running, push it further out" and "already
        // past due but an admin wants to revive it").
        LocalDate base = (movie.getScheduledEndDate() != null && movie.getScheduledEndDate().isAfter(today))
                ? movie.getScheduledEndDate()
                : today;

        movie.setScheduledEndDate(base.plusDays(days));
        movie.setExpiryWarningSent(false);

        StreamingMovie saved = streamingMovieRepository.save(movie);

        auditLogService.log(
                "EXTEND",
                "MOVIE",
                saved.getMovieName(),
                "Extended '" + saved.getMovieName() + "'s scheduled run by " + days +
                        " day(s), now ending " + saved.getScheduledEndDate() + ".");

        notificationService.notifyAdmins(
                NotificationType.MOVIE_EXTENDED,
                "Movie schedule extended",
                "\"" + saved.getMovieName() + "\"'s run was extended by " + days +
                        " day(s) — now ending " + saved.getScheduledEndDate() + ".");

        return saved;
    }

    @Override
    @Transactional
    public void processScheduledExpiries() {

        LocalDate today = LocalDate.now();
        LocalDate warningCutoff = today.plusDays(EXPIRY_WARNING_DAYS);

        List<StreamingMovie> scheduled = streamingMovieRepository.findByActiveTrueAndScheduledEndDateIsNotNull();

        for (StreamingMovie movie : scheduled) {
            LocalDate endDate = movie.getScheduledEndDate();

            if (endDate.isBefore(today)) {
                // Past due and never extended — auto-remove it (movie + shows).
                autoRemoveExpiredMovie(movie);
            } else if (!Boolean.TRUE.equals(movie.getExpiryWarningSent()) && !endDate.isAfter(warningCutoff)) {
                // Coming up within the warning window and not yet warned about.
                notificationService.notifyAdmins(
                        NotificationType.MOVIE_EXPIRING,
                        "Movie run ending soon",
                        "\"" + movie.getMovieName() + "\"'s scheduled run ends on " + endDate +
                                ". Extend it from the Movies page, or it (and its shows) will be " +
                                "automatically removed on that date.");
                movie.setExpiryWarningSent(true);
                streamingMovieRepository.save(movie);
            }
        }
    }

    // Removes a movie whose scheduled run expired without being extended:
    // cancels (full refund + user notification) then deletes every one of
    // its shows via the normal ShowService flow, then archives/deletes the
    // movie itself exactly like a manual deleteMovie would. Runs from the
    // scheduler, not an admin request, so the audit trail records "system".
    private void autoRemoveExpiredMovie(StreamingMovie movie) {

        List<Show> shows = showRepository.findByMovieId(movie.getId());
        for (Show show : shows) {
            try {
                if (!Boolean.TRUE.equals(show.getCancelled())) {
                    // Refunds any confirmed bookings in full and notifies
                    // those users, then frees the seats.
                    showService.cancelShow(show.getId());
                }
                showService.deleteShow(show.getId());
            } catch (Exception e) {
                log.error("Failed to remove show {} while auto-removing expired movie {}",
                        show.getId(), movie.getId(), e);
            }
        }

        PreviousMovie previousMovie = PreviousMovie.builder()
                .movieName(movie.getMovieName())
                .genre(movie.getGenre())
                .language(movie.getLanguage())
                .certificate(movie.getCertificate())
                .duration(movie.getDuration())
                .screenNumber(movie.getScreenNumber())
                .releaseDate(movie.getReleaseDate())
                .description(movie.getDescription())
                .active(false)
                .build();
        previousMovieRepository.save(previousMovie);

        streamingMovieRepository.delete(movie);

        auditLogService.log(
                "AUTO_REMOVE",
                "MOVIE",
                movie.getMovieName(),
                "Scheduled run for '" + movie.getMovieName() + "' ended on " + movie.getScheduledEndDate() +
                        " without being extended — automatically removed along with its " +
                        shows.size() + " show(s).");

        notificationService.notifyAdmins(
                NotificationType.MOVIE_AUTO_REMOVED,
                "Movie auto-removed",
                "\"" + movie.getMovieName() + "\"'s scheduled run ended without extension and was " +
                        "automatically removed from movies and shows.");
    }
}