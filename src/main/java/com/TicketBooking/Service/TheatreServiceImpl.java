package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.TheatreRequest;
import com.TicketBooking.DTO.Response.TheatreResponse;
import com.TicketBooking.Exception.ConflictException;
import com.TicketBooking.Model.Theatre;
import com.TicketBooking.Repository.ScreenRepository;
import com.TicketBooking.Repository.TheatreRepository;
import com.TicketBooking.Service.TheatreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TheatreServiceImpl implements TheatreService {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final AuditLogService auditLogService;

    @Override
    public TheatreResponse addTheatre(TheatreRequest request) {

        if (theatreRepository.existsByTheatreName(request.getTheatreName())) {
            throw new RuntimeException("Theatre already exists");
        }

        Theatre theatre = Theatre.builder()
                .theatreName(request.getTheatreName())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .build();

        theatre = theatreRepository.save(theatre);

        auditLogService.log(
                "ADD",
                "THEATRE",
                theatre.getTheatreName(),
                "Added theatre '" + theatre.getTheatreName() + "' in " + theatre.getCity() + ".");

        return mapToResponse(theatre);
    }

    @Override
    public TheatreResponse updateTheatre(Long id, TheatreRequest request) {

        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theatre not found"));

        theatre.setTheatreName(request.getTheatreName());
        theatre.setAddress(request.getAddress());
        theatre.setCity(request.getCity());
        theatre.setPhone(request.getPhone());

        theatre = theatreRepository.save(theatre);

        auditLogService.log(
                "UPDATE",
                "THEATRE",
                theatre.getTheatreName(),
                "Updated theatre '" + theatre.getTheatreName() + "'.");

        return mapToResponse(theatre);
    }

    @Override
    public TheatreResponse getTheatre(Long id) {

        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theatre not found"));

        return mapToResponse(theatre);
    }

    @Override
    public List<TheatreResponse> getAllTheatres() {

        return theatreRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteTheatre(Long id) {

        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theatre not found"));

        long screenCount = screenRepository.countByTheatreId(id);
        if (screenCount > 0) {
            throw new ConflictException(
                    "This theatre still has " + screenCount +
                            " screen(s). Remove those first before deleting the theatre.");
        }

        theatreRepository.delete(theatre);

        auditLogService.log(
                "DELETE",
                "THEATRE",
                theatre.getTheatreName(),
                "Removed theatre '" + theatre.getTheatreName() + "'.");
    }

    private TheatreResponse mapToResponse(Theatre theatre) {

        return TheatreResponse.builder()
                .id(theatre.getId())
                .theatreName(theatre.getTheatreName())
                .address(theatre.getAddress())
                .city(theatre.getCity())
                .phone(theatre.getPhone())
                .build();
    }
}
