package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.AdminRequest;
import com.TicketBooking.DTO.Response.AddAdminResponse;
import com.TicketBooking.DTO.Response.AdminResponse;
import com.TicketBooking.Model.Role;
import com.TicketBooking.Model.User;
import com.TicketBooking.Repository.RefreshTokenRepository;
import com.TicketBooking.Repository.RoleRepository;
import com.TicketBooking.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public AdminResponse addAdmin(AdminRequest request) {

        Role adminRole = roleRepository.findById(2)
                .orElseThrow(() ->
                        new RuntimeException("Admin role not found"));

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user != null) {

            if (user.getRole().getId().equals(2)) {
                return new AdminResponse("User is already an Admin");
            }

            user.setRole(adminRole);
            userRepository.save(user);

            auditLogService.log(
                    "ADD",
                    "ADMIN",
                    user.getEmail(),
                    "Promoted user to ADMIN"
            );

            return new AdminResponse("User promoted to Admin successfully");        }

        User newAdmin = User.builder()
                .email(request.getEmail())
                .role(adminRole)
                .build();

        userRepository.save(newAdmin);

        auditLogService.log(
                "ADD",
                "ADMIN",
                newAdmin.getEmail(),
                "Created new ADMIN account"
        );

        return new AdminResponse("New Admin created successfully");
    }

    @Override
    public List<AddAdminResponse> getAllAdmins() {

        return userRepository.findByRoleId(2)
                .stream()
                .map(user -> new AddAdminResponse(
                        user.getId(),
                        user.getEmail()
                ))
                .toList();
    }

    @Transactional
    @Override
    public AdminResponse deleteAdmin(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!user.getRole().getId().equals(2)) {
            throw new RuntimeException("User is not an Admin");
        }

        Role userRole = roleRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("User role not found"));

        refreshTokenRepository.deleteByUserId(user.getId());

        user.setRole(userRole);
        userRepository.save(user);

        auditLogService.log(
                "REMOVE",
                "ADMIN",
                user.getEmail(),
                "Removed ADMIN privileges"
        );

        return new AdminResponse("Admin removed successfully");
    }
}
