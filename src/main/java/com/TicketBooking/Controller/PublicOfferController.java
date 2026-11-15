package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Response.OfferResponse;
import com.TicketBooking.Service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class PublicOfferController {
    private final OfferService offerService;

    @GetMapping
    public ResponseEntity<List<OfferResponse>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }
}