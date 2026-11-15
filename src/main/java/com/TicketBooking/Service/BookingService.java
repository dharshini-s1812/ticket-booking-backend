package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.CreateBookingRequest;
import com.TicketBooking.DTO.Request.PaymentRequest;
import com.TicketBooking.DTO.Response.BookingResponse;
import com.TicketBooking.DTO.Response.PaymentResponse;

import java.util.List;

public interface BookingService {

    // userEmail comes from the JWT (Authentication#getName()), matching
    // the pattern already used in AuthController.
    BookingResponse createBooking(String userEmail, CreateBookingRequest request);

    PaymentResponse payForBooking(String userEmail, Long bookingId, PaymentRequest request);

    BookingResponse cancelBooking(String userEmail, Long bookingId);

    BookingResponse getBooking(String userEmail, Long bookingId);

    List<BookingResponse> getMyBookings(String userEmail);

    // Releases abandoned holds (PENDING bookings past the hold window) —
    // called by the scheduled cleanup job.
    void releaseExpiredHolds();
}
