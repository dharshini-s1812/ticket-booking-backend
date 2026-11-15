package com.TicketBooking.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String email, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Verify Your Email");
        message.setText(
                "Welcome to Velvet Reel!\n\n" +
                        "Your verification code is: " + otp +
                        "\n\nThis code is valid for 10 minutes.\n" +
                        "Do not share this code with anyone."
        );

        mailSender.send(message);
    }

    @Override
    public void sendTicketEmail(String email, String movieName, byte[] ticketImage, String attachmentName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // multipart=true so we can attach the ticket image alongside the text body
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Your e-Ticket for \"" + movieName + "\" — Velvet Reel");
            helper.setText(
                    "Hi,\n\n" +
                            "Payment received — your booking for \"" + movieName + "\" is confirmed!\n\n" +
                            "Your e-ticket is attached to this email as an image. It includes a QR code — " +
                            "please show it (on your phone or printed) at the theatre entrance for entry.\n\n" +
                            "Enjoy the show!\n\n" +
                            "- Team Velvet Reel",
                    false
            );
            helper.addAttachment(attachmentName, new ByteArrayResource(ticketImage), "image/png");

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // A failure to email the ticket shouldn't undo an already-successful
            // payment, so this is logged rather than propagated — callers should
            // not let this exception roll back the booking/payment transaction.
            log.error("Failed to send ticket email to {}", email, e);
            throw new RuntimeException("Failed to send ticket email", e);
        }
    }
}
