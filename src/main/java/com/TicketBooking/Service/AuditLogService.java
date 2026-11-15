package com.TicketBooking.Service;

import com.TicketBooking.DTO.Response.AuditLogResponse;

import java.util.List;

public interface AuditLogService {

    void log(String action,
             String entityType,
             String entityName,
             String description);

    List<AuditLogResponse> getAllLogs();

}
