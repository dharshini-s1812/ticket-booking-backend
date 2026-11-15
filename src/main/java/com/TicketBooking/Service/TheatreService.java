package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.TheatreRequest;
import com.TicketBooking.DTO.Response.TheatreResponse;

import java.util.List;

public interface TheatreService {

    TheatreResponse addTheatre(TheatreRequest request);

    TheatreResponse updateTheatre(Long id, TheatreRequest request);

    TheatreResponse getTheatre(Long id);

    List<TheatreResponse> getAllTheatres();

    void deleteTheatre(Long id);
}
