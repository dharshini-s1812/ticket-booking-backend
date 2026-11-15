package com.TicketBooking.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "theatres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theatre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String theatreName;
    @Column(nullable = false)
    private String address;
    @Column(nullable =false)
    private String city;
    @Column(nullable = false)
    private String phone;
}