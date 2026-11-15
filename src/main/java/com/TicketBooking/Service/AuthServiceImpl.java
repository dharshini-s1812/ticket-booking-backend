package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.*;
import com.TicketBooking.DTO.Response.AuthResponse;
import com.TicketBooking.DTO.Response.SessionResponse;
import com.TicketBooking.Model.OtpVerification;
import com.TicketBooking.Model.NotificationType;
import com.TicketBooking.Model.RefreshToken;
import com.TicketBooking.Model.Role;
import com.TicketBooking.Model.User;
import com.TicketBooking.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpVerificationRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final NotificationService notificationService;

    private static final long REFRESH_TOKEN_VALID_DAYS = 7;

    // Don't send another "welcome back" notification if the user already
    // got one within this window — otherwise someone who logs in five
    // times a day gets five identical notifications.
    private static final int LOGIN_NOTIFICATION_THROTTLE_HOURS = 12;

    @Override
    @Transactional
    public void sendVerificationEmail(EmailRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        // Find existing user or create a new one
        User user = userRepository.findByEmail(email).orElseGet(() -> {

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Default role not found."));

            User newUser = User.builder()
                    .email(email)
                    .role(userRole)
                    .build();

            return userRepository.save(newUser);
        });

        // Generate OTP
        String otp = otpService.generateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();

        otpRepository.save(otpVerification);

        // Send OTP via email
        emailService.sendVerificationEmail(email, otp);
    }


    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {

        User user;

        // ================= EMAIL OTP =================
        if (request.getEmail() != null && !request.getEmail().isBlank()) {

            String email = request.getEmail().trim().toLowerCase();

            OtpVerification otpVerification = otpRepository
                    .findTopByEmailOrderByCreatedAtDesc(email)
                    .orElseThrow(() -> new RuntimeException("No OTP found for this email."));

            if (otpVerification.isVerified()) {
                throw new RuntimeException("OTP has already been used.");
            }

            if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("OTP has expired.");
            }


            otpVerification.setVerified(true);
            otpRepository.save(otpVerification);

            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found."));

            if (user.getEmailVerifiedAt() == null) {
                user.setEmailVerifiedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        } else {
            throw new RuntimeException("Email is required.");
        }

        return issueTokens(user, "Login successful.");

    }


    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token."));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token expired. Please log in again.");
        }

        if (!jwtService.validateToken(storedToken.getToken())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token is no longer valid. Please log in again.");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(storedToken.getToken())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .message("Token refreshed successfully.")
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    public SessionResponse getSession(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        return SessionResponse.builder()
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .build();
    }

    private AuthResponse issueTokens(User user, String message) {

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALID_DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);

        notificationService.notifyUserThrottled(
                user,
                NotificationType.LOGIN,
                "New login",
                "You just logged in to VelvetReel.",
                LOGIN_NOTIFICATION_THROTTLE_HOURS);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .message(message)
                .build();
    }
}