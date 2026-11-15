package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String adminEmail;
    private String action;
    private String entityType;
    private String entityName;
    @Column(length = 1000)
    private String description;
    private LocalDateTime createdAt;
}
