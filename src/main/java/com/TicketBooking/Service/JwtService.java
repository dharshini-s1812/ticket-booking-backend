package com.TicketBooking.Service;

import com.TicketBooking.Model.User;

public interface JwtService {

    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    String extractUsername(String token);
    boolean validateToken(String token);
}
