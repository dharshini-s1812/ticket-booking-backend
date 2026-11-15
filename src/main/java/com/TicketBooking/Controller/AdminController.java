package com.TicketBooking.Controller;

import com.TicketBooking.DTO.Request.AdminRequest;
import com.TicketBooking.DTO.Response.AddAdminResponse;
import com.TicketBooking.DTO.Response.AdminResponse;
import com.TicketBooking.Model.User;
import com.TicketBooking.Service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/add")
    public ResponseEntity<AdminResponse> addAdmin(
            @RequestBody AdminRequest request) {

        return ResponseEntity.ok(
                adminService.addAdmin(request)
        );
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AddAdminResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AdminResponse> deleteAdmin(@PathVariable Long id) {

        return ResponseEntity.ok(
                adminService.deleteAdmin(id)
        );

    }

}
