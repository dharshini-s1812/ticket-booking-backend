package com.TicketBooking.Exception;

// Thrown when a request is valid in isolation but can't be applied given
// the current state — e.g. a seat that's already held/booked, or a show
// time that overlaps another show on the same screen.
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
