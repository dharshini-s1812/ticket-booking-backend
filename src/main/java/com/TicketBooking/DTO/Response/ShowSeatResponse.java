package com.TicketBooking.DTO.Response;

import com.TicketBooking.Model.SeatStatus;
import com.TicketBooking.Model.SeatType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowSeatResponse {
    private Long showSeatId;
    private Long seatId;
    private String seatNumber;
    private SeatType seatType;
    private Double price;
    private SeatStatus status;
}
