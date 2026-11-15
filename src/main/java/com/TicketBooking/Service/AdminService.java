package com.TicketBooking.Service;

import com.TicketBooking.DTO.Request.AdminRequest;
import com.TicketBooking.DTO.Response.AddAdminResponse;
import com.TicketBooking.DTO.Response.AdminResponse;

import java.util.List;

public interface AdminService {
    AdminResponse addAdmin(AdminRequest request);
    List<AddAdminResponse> getAllAdmins();
    AdminResponse deleteAdmin(Long userId);
}
