package com.TicketBooking.Service;

public interface EmailService {

    void sendVerificationEmail(String email, String otp);

    /**
     * Emails a booking confirmation with the e-ticket (movie/seat/QR details
     * rendered onto a PNG) attached as an inline image.
     *
     * @param email        recipient
     * @param movieName    used in the subject/body
     * @param ticketImage  PNG bytes of the rendered ticket (see TicketImageService)
     * @param attachmentName file name for the attachment, e.g. "ticket-42.png"
     */
    void sendTicketEmail(String email, String movieName, byte[] ticketImage, String attachmentName);
}
