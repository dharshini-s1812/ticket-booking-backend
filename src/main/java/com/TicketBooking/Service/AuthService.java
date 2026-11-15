package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.*;
import com.TicketBooking.DTO.Response.AuthResponse;
import com.TicketBooking.DTO.Response.SessionResponse;

public interface AuthService {
    void sendVerificationEmail(EmailRequest request);
    AuthResponse verifyOtp(VerifyOtpRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String refreshToken);

    // Looks the caller's role up fresh from the DB (never from the JWT,
    // which has no role claim) so an admin who was just demoted shows up
    // as ROLE_USER on their very next poll, even though their still-valid
    // access token has no idea that happened.
    SessionResponse getSession(String email);
}
