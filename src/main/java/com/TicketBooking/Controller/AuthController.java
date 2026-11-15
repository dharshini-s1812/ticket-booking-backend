package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.*;
import com.TicketBooking.DTO.Response.AuthResponse;
import com.TicketBooking.DTO.Response.MessageResponse;
import com.TicketBooking.DTO.Response.SessionResponse;
import com.TicketBooking.Service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/email")
    public ResponseEntity<?> sendVerificationEmail(
            @RequestBody EmailRequest request) {

        authService.sendVerificationEmail(request);
        return ResponseEntity.ok(new MessageResponse("Verification email sent."));
    }



    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody RefreshTokenRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully."));
    }

    // Polled by the admin panel to catch a role change (e.g. an admin
    // being demoted by another admin) mid-session. The access token stays
    // cryptographically valid for up to an hour regardless of role, so
    // this is the only way to notice the change before it expires.
    @GetMapping("/session")
    public ResponseEntity<?> getSession(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Not authenticated."));
        }

        return ResponseEntity.ok(authService.getSession(authentication.getName()));
    }
}
