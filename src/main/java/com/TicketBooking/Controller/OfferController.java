package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.OfferRequest;
import com.TicketBooking.DTO.Response.OfferResponse;
import com.TicketBooking.Service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/offers")
@RequiredArgsConstructor
public class OfferController {
    private final OfferService offerService;
    @PostMapping
    public ResponseEntity<OfferResponse> addOffer(
            @RequestBody OfferRequest request) {

        return ResponseEntity.ok(offerService.addOffer(request));
    }
    @PutMapping("/{id}")
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable Long id,
            @RequestBody OfferRequest request) {
        return ResponseEntity.ok(
                offerService.updateOffer(id, request));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOffer(
            @PathVariable Long id) {
        offerService.deleteOffer(id);
        return ResponseEntity.ok("Offer deleted successfully.");
    }
    @GetMapping
    public ResponseEntity<List<OfferResponse>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }
    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> getOffer(
            @PathVariable Long id) {
        return ResponseEntity.ok(offerService.getOffer(id));
    }
}