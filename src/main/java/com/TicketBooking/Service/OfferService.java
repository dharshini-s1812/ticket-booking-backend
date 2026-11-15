package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.OfferRequest;
import com.TicketBooking.DTO.Response.OfferResponse;
import java.util.List;

public interface OfferService {
    OfferResponse addOffer(OfferRequest request);
    OfferResponse updateOffer(Long id, OfferRequest request);
    void deleteOffer(Long id);
    List<OfferResponse> getAllOffers();
    OfferResponse getOffer(Long id);
}
