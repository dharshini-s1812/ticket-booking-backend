package com.TicketBooking.DTO.Response;

import com.TicketBooking.Model.BookingStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;

    private Long showId;
    private String movieName;
    private String theatreName;
    private String screenName;
    private LocalDate showDate;
    private LocalTime startTime;

    private List<String> seatNumbers;
    private Double totalAmount;
    private BookingStatus status;
    private LocalDateTime bookingTime;

    // Only populated once this booking has actually been refunded. Can be
    // less than totalAmount — see BookingServiceImpl.CANCELLATION_FEE_PERCENT.
    private Double refundAmount;

    // Only meaningful while status is PENDING — the hold is released back
    // to AVAILABLE automatically after this time if payment isn't completed.
    private LocalDateTime holdExpiresAt;
}