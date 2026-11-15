package com.TicketBooking.Service;

import com.TicketBooking.Model.Booking;

import java.util.List;

public interface TicketImageService {

    /**
     * Renders a single PNG "ticket stub" image for a confirmed booking:
     * movie/theatre/show details on the left, a scannable QR code on the right.
     *
     * @param booking       the confirmed booking (show, user, totalAmount, etc.)
     * @param seatNumbers   booked seat numbers, already sorted for display
     * @param transactionId the payment's transaction id, shown on the ticket
     *                      and encoded into the QR payload
     * @return PNG-encoded image bytes
     */
    byte[] generateTicketImage(Booking booking, List<String> seatNumbers, String transactionId);
}
