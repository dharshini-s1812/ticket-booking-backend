package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Response.AuditLogResponse;
import com.TicketBooking.Service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }
}
