package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Response.MessageResponse;
import com.TicketBooking.DTO.Response.NotificationResponse;
import com.TicketBooking.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in."));
        }
        return ResponseEntity.ok(notificationService.getMyNotifications(authentication.getName()));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in."));
        }
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(authentication.getName())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(Authentication authentication, @PathVariable Long id) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in."));
        }
        notificationService.markRead(authentication.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Marked as read."));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(new MessageResponse("Please log in."));
        }
        notificationService.markAllRead(authentication.getName());
        return ResponseEntity.ok(new MessageResponse("All marked as read."));
    }
}