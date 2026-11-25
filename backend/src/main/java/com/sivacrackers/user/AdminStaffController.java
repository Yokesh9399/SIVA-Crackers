package com.sivacrackers.user;

import com.sivacrackers.user.AuthDtos.CreateStaffRequest;
import com.sivacrackers.user.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff provisioning – OWNER only. Full staff management UI arrives with the admin app (Phase 5). */
@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    private final StaffService staff;

    public AdminStaffController(StaffService staff) {
        this.staff = staff;
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateStaffRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.of(staff.create(body)));
    }
}
