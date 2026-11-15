package com.TicketBooking.Service;

import com.TicketBooking.DTO.Response.AuditLogResponse;
import com.TicketBooking.Model.AuditLog;
import com.TicketBooking.Repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(String action,
                    String entityType,
                    String entityName,
                    String description) {

        // Most calls happen inside an authenticated admin request, but this
        // is also called from background schedulers (e.g. auto-removing an
        // expired movie) where there's no logged-in admin on the thread —
        // fall back to a clearly-labelled system actor instead of NPEing.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String adminEmail = (authentication != null && authentication.getName() != null
                && authentication.isAuthenticated())
                ? authentication.getName()
                : "system";

        AuditLog log = AuditLog.builder()
                .adminEmail(adminEmail)
                .action(action)
                .entityType(entityType)
                .entityName(entityName)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLogResponse> getAllLogs() {

        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .adminEmail(log.getAdminEmail())
                        .action(log.getAction())
                        .entityType(log.getEntityType())
                        .entityName(log.getEntityName())
                        .description(log.getDescription())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }
}