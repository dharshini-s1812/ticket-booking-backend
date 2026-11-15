package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Response.MessageResponse;
import com.TicketBooking.DTO.Response.ShowRunResponse;
import com.TicketBooking.Service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/show-runs")
@RequiredArgsConstructor
public class ShowRunController {

    private final ShowService showService;

    private static final Set<Integer> ALLOWED_EXTENSION_DAYS = Set.of(3, 5, 7);

    // Runs whose current end date is tomorrow and haven't been decided on
    // yet — what the admin's "extend or end" panel is built from.
    @GetMapping("/ending-soon")
    public ResponseEntity<List<ShowRunResponse>> getRunsEndingSoon() {
        return ResponseEntity.ok(showService.getRunsEndingSoon());
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<?> extendRun(@PathVariable Long id, @RequestParam int days) {
        if (!ALLOWED_EXTENSION_DAYS.contains(days)) {
            return ResponseEntity.badRequest().body(new MessageResponse("days must be 3, 5, or 7."));
        }
        return ResponseEntity.ok(showService.extendRun(id, days));
    }

    @PostMapping("/{id}/decline-extension")
    public ResponseEntity<MessageResponse> declineExtension(@PathVariable Long id) {
        showService.declineExtension(id);
        return ResponseEntity.ok(new MessageResponse("Won't ask about this run again."));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MessageResponse> cancelRun(@PathVariable Long id) {
        showService.cancelRun(id);
        return ResponseEntity.ok(new MessageResponse("Run cancelled."));
    }
}
