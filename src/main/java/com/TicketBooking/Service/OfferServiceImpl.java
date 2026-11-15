package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.OfferRequest;
import com.TicketBooking.DTO.Response.OfferResponse;
import com.TicketBooking.Model.Offer;
import com.TicketBooking.Repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final AuditLogService auditLogService;

    @Override
    public OfferResponse addOffer(OfferRequest request) {

        Offer offer = Offer.builder()
                .offerName(request.getOfferName())
                .description(request.getDescription())
                .discountPercentage(request.getDiscountPercentage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();

        Offer savedOffer = offerRepository.save(offer);
        auditLogService.log(
                "ADD",
                "OFFER",
                savedOffer.getOfferName(),
                "Added offer '" + savedOffer.getOfferName() + "' with "
                        + savedOffer.getDiscountPercentage() + "% discount."
        );

        return mapToResponse(savedOffer);
    }

    @Override
    public OfferResponse updateOffer(Long id, OfferRequest request) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        offer.setOfferName(request.getOfferName());
        offer.setDescription(request.getDescription());
        offer.setDiscountPercentage(request.getDiscountPercentage());
        offer.setStartDate(request.getStartDate());
        offer.setEndDate(request.getEndDate());

        Offer updatedOffer = offerRepository.save(offer);

        auditLogService.log(
                "UPDATE",
                "OFFER",
                updatedOffer.getOfferName(),
                "Updated offer '" + updatedOffer.getOfferName() + "'."
        );

        return mapToResponse(updatedOffer);
    }

    @Override
    public void deleteOffer(Long id) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        offer.setActive(false);
        offerRepository.save(offer);

        auditLogService.log(
                "DELETE",
                "OFFER",
                offer.getOfferName(),
                "Removed offer '" + offer.getOfferName() + "'."
        );
    }

    @Override
    public List<OfferResponse> getAllOffers() {

        return offerRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public OfferResponse getOffer(Long id) {

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        return mapToResponse(offer);
    }

    private OfferResponse mapToResponse(Offer offer) {

        return OfferResponse.builder()
                .id(offer.getId())
                .offerName(offer.getOfferName())
                .description(offer.getDescription())
                .discountPercentage(offer.getDiscountPercentage())
                .startDate(offer.getStartDate())
                .endDate(offer.getEndDate())
                .active(offer.isActive())
                .build();
    }
}
