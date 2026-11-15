package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.CreateBookingRequest;
import com.TicketBooking.DTO.Request.PaymentRequest;
import com.TicketBooking.DTO.Response.BookingResponse;
import com.TicketBooking.DTO.Response.MessageResponse;
import com.TicketBooking.DTO.Response.PaymentResponse;
import com.TicketBooking.Service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> createBooking(Authentication authentication, @RequestBody CreateBookingRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in to book tickets."));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(authentication.getName(), request));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(Authentication authentication, @PathVariable Long id, @RequestBody(required = false) PaymentRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in to complete payment."));
        }
        PaymentRequest body = request == null ? new PaymentRequest() : request;
        return ResponseEntity.ok(bookingService.payForBooking(authentication.getName(), id, body));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(Authentication authentication, @PathVariable Long id) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in."));
        }
        return ResponseEntity.ok(bookingService.cancelBooking(authentication.getName(), id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(Authentication authentication, @PathVariable Long id) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in."));
        }
        return ResponseEntity.ok(bookingService.getBooking(authentication.getName(), id));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyBookings(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in."));
        }
        return ResponseEntity.ok(bookingService.getMyBookings(authentication.getName()));
    }
}
