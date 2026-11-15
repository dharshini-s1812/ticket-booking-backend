package com.TicketBooking.Service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generateOtp() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }
}
