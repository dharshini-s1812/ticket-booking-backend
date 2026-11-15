package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.ScreenRequest;
import com.TicketBooking.DTO.Response.ScreenResponse;
import com.TicketBooking.Exception.ConflictException;
import com.TicketBooking.Model.Screen;
import com.TicketBooking.Model.Seat;
import com.TicketBooking.Model.SeatType;
import com.TicketBooking.Model.Theatre;
import com.TicketBooking.Repository.ScreenRepository;
import com.TicketBooking.Repository.SeatRepository;
import com.TicketBooking.Repository.ShowRepository;
import com.TicketBooking.Repository.TheatreRepository;
import com.TicketBooking.Service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final AuditLogService auditLogService;

    // Base surcharge baked into Seat.price for each type. Shows add this on
    // top of their own basePrice when a booking is priced — see
    // BookingServiceImpl.
    private static final double PREMIUM_SURCHARGE = 80.0;
    private static final double RECLINER_SURCHARGE = 150.0;

    // Row width scales with screen size instead of a fixed 10 — a 20-seat
    // screening room and a 300-seat multiplex shouldn't both be laid out
    // as 10-wide grids (the first looks cramped into 2 rows, the second
    // becomes an unreadable 30-row wall).
    private static int seatsPerRowFor(int totalSeats) {
        if (totalSeats <= 40) return 8;
        if (totalSeats <= 100) return 10;
        if (totalSeats <= 200) return 12;
        return 14;
    }

    @Override
    @Transactional
    public ScreenResponse addScreen(ScreenRequest request) {

        Theatre theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new RuntimeException("Theatre not found"));

        Screen screen = Screen.builder()
                .screenName(request.getScreenName())
                .totalSeats(request.getTotalSeats())
                .theatre(theatre)
                .build();

        screen = screenRepository.save(screen);

        generateSeats(screen);

        auditLogService.log(
                "ADD",
                "SCREEN",
                screen.getScreenName(),
                "Added screen '" + screen.getScreenName() + "' (" + screen.getTotalSeats() +
                        " seats) to " + theatre.getTheatreName() + ".");

        return mapToResponse(screen);
    }

    @Override
    @Transactional
    public ScreenResponse updateScreen(Long id, ScreenRequest request) {

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Screen not found"));

        Theatre theatre = theatreRepository.findById(request.getTheatreId())
                .orElseThrow(() -> new RuntimeException("Theatre not found"));

        boolean seatCountChanged = !request.getTotalSeats().equals(screen.getTotalSeats());

        screen.setScreenName(request.getScreenName());
        screen.setTotalSeats(request.getTotalSeats());
        screen.setTheatre(theatre);

        screen = screenRepository.save(screen);

        if (seatCountChanged) {
            long currentSeatCount = seatRepository.countByScreenId(screen.getId());
            int newTotal = request.getTotalSeats();

            if (currentSeatCount == 0) {
                // Nothing generated yet — safe to just generate at the new count.
                generateSeats(screen);
            } else if (newTotal > currentSeatCount) {
                // Growing the screen: existing seats (and anything already
                // booked against them) are untouched — we only append the
                // new ones. Safe even with shows already scheduled.
                appendSeats(screen, (int) currentSeatCount, newTotal);
            } else {
                // Shrinking means some existing seats have to go away,
                // which could delete ones that are already booked on a
                // scheduled show. Only allow that when nothing depends on
                // the current layout yet.
                int showCount = showRepository.findByScreenId(screen.getId()).size();
                if (showCount > 0) {
                    throw new ConflictException(
                            "Can't reduce the seat count to " + newTotal + ": this screen has " + showCount +
                                    " show(s) scheduled on it and some of the seats being removed could " +
                                    "already be booked. Remove those shows first, then reduce the seat count. " +
                                    "(Increasing the seat count is always safe and doesn't have this restriction.)");
                }
                seatRepository.deleteByScreenId(screen.getId());
                generateSeats(screen);
            }
        }

        auditLogService.log(
                "UPDATE",
                "SCREEN",
                screen.getScreenName(),
                "Updated screen '" + screen.getScreenName() + "' (" + screen.getTheatre().getTheatreName() + ").");

        return mapToResponse(screen);
    }

    // Adds new rows on top of a screen's existing seats without touching
    // them — used when increasing totalSeats on a screen that already has
    // shows scheduled. New rows all come in as REGULAR (no premium/recliner
    // guessing for a partial layout); existing seats/rows are left exactly
    // as they were, so nothing already booked shifts under anyone. Note:
    // shows created *before* this runs keep their original (smaller) seat
    // map snapshot — only shows created after will see the new seats.
    private void appendSeats(Screen screen, int existingCount, int newTotal) {
        List<Seat> existing = seatRepository.findByScreenId(screen.getId());
        char lastRow = existing.stream()
                .map(s -> s.getSeatNumber().charAt(0))
                .max(Character::compareTo)
                .orElse((char) ('A' - 1));

        int seatsPerRow = seatsPerRowFor(newTotal);
        int remaining = newTotal - existingCount;
        List<Seat> newSeats = new ArrayList<>();

        char row = (char) (lastRow + 1);
        while (remaining > 0) {
            int seatsInRow = Math.min(seatsPerRow, remaining);
            for (int n = 1; n <= seatsInRow; n++) {
                newSeats.add(Seat.builder()
                        .seatNumber(row + String.valueOf(n))
                        .seatType(SeatType.REGULAR)
                        .price(0.0)
                        .screen(screen)
                        .build());
            }
            remaining -= seatsInRow;
            row++;
        }

        seatRepository.saveAll(newSeats);
    }

    private void generateSeats(Screen screen) {
        int total = screen.getTotalSeats();
        int seatsPerRow = seatsPerRowFor(total);
        int rows = (int) Math.ceil(total / (double) seatsPerRow);

        // Proportional, not fixed: ~15% of rows are PREMIUM (at least 1 if
        // there's room), ~10% are RECLINER (at least 1 if there's room left
        // after premium). Tiny screens (1-2 rows) just stay REGULAR — no
        // room to carve out a "premium row" of one.
        int premiumRows = rows >= 3 ? Math.max(1, (int) Math.round(rows * 0.15)) : 0;
        int reclinerRows = rows >= 4 ? Math.max(1, (int) Math.round(rows * 0.10)) : 0;
        // Never let premium+recliner eat the whole layout.
        while (premiumRows + reclinerRows >= rows && (premiumRows > 0 || reclinerRows > 0)) {
            if (reclinerRows > 0) reclinerRows--;
            else premiumRows--;
        }

        List<Seat> seats = new ArrayList<>();
        int remaining = total;

        for (int r = 0; r < rows && remaining > 0; r++) {
            char rowLetter = (char) ('A' + r);
            int seatsInRow = Math.min(seatsPerRow, remaining);

            SeatType type;
            double surcharge;
            if (r < premiumRows) {
                type = SeatType.PREMIUM;
                surcharge = PREMIUM_SURCHARGE;
            } else if (r >= rows - reclinerRows) {
                type = SeatType.RECLINER;
                surcharge = RECLINER_SURCHARGE;
            } else {
                type = SeatType.REGULAR;
                surcharge = 0.0;
            }

            for (int n = 1; n <= seatsInRow; n++) {
                seats.add(Seat.builder()
                        .seatNumber(rowLetter + String.valueOf(n))
                        .seatType(type)
                        .price(surcharge)
                        .screen(screen)
                        .build());
            }
            remaining -= seatsInRow;
        }

        seatRepository.saveAll(seats);
    }

    @Override
    @Transactional
    public ScreenResponse regenerateSeats(Long id) {

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Screen not found"));

        long actual = seatRepository.countByScreenId(id);
        if (actual > 0) {
            throw new ConflictException(
                    "This screen already has " + actual + " seat(s) generated — nothing to fix. " +
                            "Edit the screen if you need to change its seat count.");
        }

        generateSeats(screen);

        return mapToResponse(screen);
    }

    @Override
    public ScreenResponse getScreen(Long id) {

        return mapToResponse(
                screenRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Screen not found"))
        );
    }

    @Override
    public List<ScreenResponse> getAllScreens() {

        return screenRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteScreen(Long id) {

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Screen not found"));

        // Shows FK to screen with nullable = false, so any show scheduled on
        // this screen would block deletion at the DB level with a raw
        // constraint-violation message. Check up front instead so the admin
        // gets a clear reason.
        int showCount = showRepository.findByScreenId(id).size();
        if (showCount > 0) {
            throw new ConflictException(
                    "This screen still has " + showCount +
                            " show(s) scheduled on it. Remove those shows first before deleting the screen.");
        }

        // Seats FK to screen with nullable = false, so they'd block deletion
        // otherwise.
        seatRepository.deleteByScreenId(id);
        screenRepository.delete(screen);

        auditLogService.log(
                "DELETE",
                "SCREEN",
                screen.getScreenName(),
                "Removed screen '" + screen.getScreenName() + "' from " + screen.getTheatre().getTheatreName() + ".");
    }

    private ScreenResponse mapToResponse(Screen screen) {

        long actualSeatCount = seatRepository.countByScreenId(screen.getId());

        return ScreenResponse.builder()
                .id(screen.getId())
                .screenName(screen.getScreenName())
                .totalSeats(screen.getTotalSeats())
                .actualSeatCount(actualSeatCount)
                .misconfigured(actualSeatCount == 0)
                .theatreId(screen.getTheatre().getId())
                .theatreName(screen.getTheatre().getTheatreName())
                .build();
    }
}