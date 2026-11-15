package com.TicketBooking;

import com.TicketBooking.Model.Role;
import com.TicketBooking.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRole(1, "ROLE_USER");
        seedRole(2, "ROLE_ADMIN");
    }

    private void seedRole(int id, String name) {
        roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder()
                    .id(id)
                    .name(name)
                    .build();
            return roleRepository.save(role);
        });
    }
}
