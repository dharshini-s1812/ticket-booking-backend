package com.TicketBooking.DTO.Response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AuditLogResponse {

    private Long id;
    private String adminEmail;
    private String action;
    private String entityType;
    private String entityName;
    private String description;
    private LocalDateTime createdAt;
}
