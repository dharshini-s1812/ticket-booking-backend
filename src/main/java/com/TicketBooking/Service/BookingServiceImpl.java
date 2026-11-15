package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.CreateBookingRequest;
import com.TicketBooking.DTO.Request.PaymentRequest;
import com.TicketBooking.DTO.Response.BookingResponse;
import com.TicketBooking.DTO.Response.PaymentResponse;
import com.TicketBooking.Exception.BadRequestException;
import com.TicketBooking.Exception.ConflictException;
import com.TicketBooking.Exception.ResourceNotFoundException;
import com.TicketBooking.Model.*;
import com.TicketBooking.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int HOLD_MINUTES = 10;

    // A customer cancelling their own confirmed booking doesn't get a full
    // refund — this fee is deducted. Admin-initiated cancellations (a whole
    // show pulled by the cinema, see ShowServiceImpl.cancelShow) are
    // different: full refund, no fee, since that wasn't the customer's
    // choice.
    private static final double CANCELLATION_FEE_PERCENT = 0.20;

    private final BookingRepository bookingRepository;
    private final BookedSeatRepository bookedSeatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TicketImageService ticketImageService;

    @Override
    @Transactional
    public BookingResponse createBooking(String userEmail, CreateBookingRequest request) {

        if (request.getShowSeatIds() == null || request.getShowSeatIds().isEmpty()) {
            throw new BadRequestException("Select at least one seat.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found"));

        if (Boolean.TRUE.equals(show.getCancelled())) {
            throw new ConflictException("This show has been cancelled and can no longer be booked.");
        }

        // Pessimistic lock: whichever request gets here first holds these
        // rows until its transaction commits/rolls back, so a second
        // concurrent request for the same seats blocks here and then sees
        // the now-BLOCKED status below instead of racing past this check.
        List<ShowSeat> showSeats = showSeatRepository.findAllForUpdate(request.getShowSeatIds());

        if (showSeats.size() != request.getShowSeatIds().size()) {
            throw new ResourceNotFoundException("One or more selected seats don't exist.");
        }

        for (ShowSeat ss : showSeats) {
            if (!ss.getShow().getId().equals(show.getId())) {
                throw new BadRequestException("Seat " + ss.getSeat().getSeatNumber() + " doesn't belong to this show.");
            }
            if (ss.getStatus() != SeatStatus.AVAILABLE) {
                throw new ConflictException("Seat " + ss.getSeat().getSeatNumber() + " is no longer available. Please reselect your seats.");
            }
        }

        double total = 0.0;
        for (ShowSeat ss : showSeats) {
            ss.setStatus(SeatStatus.BLOCKED);
            total += show.getBasePrice() + ss.getSeat().getPrice();
        }
        showSeatRepository.saveAll(showSeats);

        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .bookingTime(LocalDateTime.now())
                .totalAmount(total)
                .status(BookingStatus.PENDING)
                .build();
        booking = bookingRepository.save(booking);

        for (ShowSeat ss : showSeats) {
            BookedSeat bookedSeat = BookedSeat.builder()
                    .booking(booking)
                    .showSeat(ss)
                    .price(show.getBasePrice() + ss.getSeat().getPrice())
                    .build();
            bookedSeatRepository.save(bookedSeat);
        }

        return mapToResponse(booking);
    }

    @Override
    @Transactional
    public PaymentResponse payForBooking(String userEmail, Long bookingId, PaymentRequest request) {

        Booking booking = getOwnedBooking(userEmail, bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("This booking isn't awaiting payment.");
        }

        if (booking.getBookingTime().plusMinutes(HOLD_MINUTES).isBefore(LocalDateTime.now())) {
            releaseHold(booking);
            throw new ConflictException("Your seat hold expired. Please select seats again.");
        }

        // Simulated payment gateway — always succeeds. Swap this for a real
        // provider integration (webhook-driven) when one is wired up.
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .paymentMethod(request.getPaymentMethod() == null ? "SIMULATED" : request.getPaymentMethod())
                .paymentStatus(PaymentStatus.SUCCESS)
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .paymentTime(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        List<BookedSeat> bookedSeats = bookedSeatRepository.findByBookingId(booking.getId());
        List<ShowSeat> showSeats = bookedSeats.stream().map(BookedSeat::getShowSeat).toList();
        showSeats.forEach(ss -> ss.setStatus(SeatStatus.BOOKED));
        showSeatRepository.saveAll(showSeats);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        Show show = booking.getShow();
        List<String> seatNumbers = bookedSeats.stream()
                .map(bs -> bs.getShowSeat().getSeat().getSeatNumber())
                .sorted()
                .toList();
        notificationService.notifyUser(
                booking.getUser(),
                NotificationType.BOOKING_CONFIRMED,
                "Booking confirmed",
                "Your booking for \"" + show.getMovie().getMovieName() + "\" on " + show.getShowDate() +
                        " at " + show.getStartTime() + " is confirmed. Seats: " + String.join(", ", seatNumbers) + ".");

        // Best-effort: render the e-ticket (with QR code) and email it. This runs
        // after payment/booking are already persisted, and is deliberately
        // swallowed on failure — a flaky mail send shouldn't roll back a
        // successful payment. The user can still see their booking in-app.
        try {
            byte[] ticketImage = ticketImageService.generateTicketImage(booking, seatNumbers, payment.getTransactionId());
            emailService.sendTicketEmail(
                    booking.getUser().getEmail(),
                    show.getMovie().getMovieName(),
                    ticketImage,
                    "ticket-" + booking.getId() + ".png");
        } catch (Exception e) {
            log.error("Failed to generate/send e-ticket for booking {}", booking.getId(), e);
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(booking.getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionId(payment.getTransactionId())
                .paymentTime(payment.getPaymentTime())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String userEmail, Long bookingId) {

        Booking booking = getOwnedBooking(userEmail, bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("This booking is already cancelled.");
        }

        // CONFIRMED shows can only be cancelled ahead of the show start —
        // once it's started, cancellation no longer makes sense.
        Double refund = null;
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            LocalDateTime showStart = LocalDateTime.of(booking.getShow().getShowDate(), booking.getShow().getStartTime());
            if (showStart.isBefore(LocalDateTime.now())) {
                throw new ConflictException("This show has already started — it can't be cancelled.");
            }
            var paymentOpt = paymentRepository.findByBookingId(booking.getId());
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                refund = payment.getAmount() * (1 - CANCELLATION_FEE_PERCENT);
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                payment.setRefundAmount(refund);
                paymentRepository.save(payment);
            }
        }

        releaseHold(booking);

        Show show = booking.getShow();
        String refundNote = refund != null
                ? " ₹" + refund + " will be refunded (a cancellation fee applies)."
                : "";
        notificationService.notifyUser(
                booking.getUser(),
                NotificationType.BOOKING_CANCELLED,
                "Booking cancelled",
                "Your booking for \"" + show.getMovie().getMovieName() + "\" on " + show.getShowDate() +
                        " has been cancelled." + refundNote);

        return mapToResponse(booking);
    }

    @Override
    public BookingResponse getBooking(String userEmail, Long bookingId) {
        return mapToResponse(getOwnedBooking(userEmail, bookingId));
    }

    @Override
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bookingRepository.findByUserIdOrderByBookingTimeDesc(user.getId())
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HOLD_MINUTES);
        List<Booking> expired = bookingRepository.findByStatusAndBookingTimeBefore(BookingStatus.PENDING, cutoff);
        for (Booking booking : expired) {
            releaseHold(booking);
        }
    }

    // ---- helpers ----

    private Booking getOwnedBooking(String userEmail, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Booking not found");
        }
        return booking;
    }

    private void releaseHold(Booking booking) {
        List<BookedSeat> bookedSeats = bookedSeatRepository.findByBookingId(booking.getId());
        List<ShowSeat> showSeats = bookedSeats.stream().map(BookedSeat::getShowSeat).toList();
        showSeats.forEach(ss -> ss.setStatus(SeatStatus.AVAILABLE));
        showSeatRepository.saveAll(showSeats);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        List<String> seatNumbers = bookedSeatRepository.findByBookingId(booking.getId()).stream()
                .map(bs -> bs.getShowSeat().getSeat().getSeatNumber())
                .sorted()
                .toList();

        Show show = booking.getShow();

        return BookingResponse.builder()
                .id(booking.getId())
                .showId(show.getId())
                .movieName(show.getMovie().getMovieName())
                .theatreName(show.getScreen().getTheatre().getTheatreName())
                .screenName(show.getScreen().getScreenName())
                .showDate(show.getShowDate())
                .startTime(show.getStartTime())
                .seatNumbers(seatNumbers)
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .bookingTime(booking.getBookingTime())
                .holdExpiresAt(booking.getStatus() == BookingStatus.PENDING
                        ? booking.getBookingTime().plusMinutes(HOLD_MINUTES)
                        : null)
                .refundAmount(paymentRepository.findByBookingId(booking.getId())
                        .map(Payment::getRefundAmount)
                        .orElse(null))
                .build();
    }
}