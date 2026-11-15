package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.ScreenRequest;
import com.TicketBooking.DTO.Response.ScreenResponse;

import java.util.List;

public interface ScreenService {

    ScreenResponse addScreen(ScreenRequest request);

    ScreenResponse updateScreen(Long id, ScreenRequest request);

    ScreenResponse getScreen(Long id);

    List<ScreenResponse> getAllScreens();

    void deleteScreen(Long id);

    // Fixes a screen that has 0 actual Seat rows (misconfigured) by running
    // seat generation for it. Only allowed when the screen currently has no
    // seats at all — if it already has seats, use updateScreen instead.
    ScreenResponse regenerateSeats(Long id);
}
