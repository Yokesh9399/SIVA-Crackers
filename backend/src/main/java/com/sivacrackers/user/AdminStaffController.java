package com.sivacrackers.user;

import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.user.AuthDtos.CreateStaffRequest;
import com.sivacrackers.user.StaffDtos.ResetPasswordRequest;
import com.sivacrackers.user.StaffDtos.StaffMember;
import com.sivacrackers.user.StaffDtos.StatusRequest;
import com.sivacrackers.user.StaffDtos.UpdateStaffRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Staff accounts – OWNER only. */
@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('OWNER')")
public class AdminStaffController {

    private final StaffService staff;

    public AdminStaffController(StaffService staff) {
        this.staff = staff;
    }

    /** {@code ?search=&role=OWNER|MANAGER|CASHIER&active=true|false} */
    @GetMapping
    public List<StaffMember> list(@RequestParam(required = false) String search,
                                  @RequestParam(required = false) String role,
                                  @RequestParam(required = false) Boolean active) {
        Role r = null;
        if (role != null && !role.isBlank()) {
            try {
                r = Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("role", "must be OWNER, MANAGER or CASHIER");
            }
            if (!StaffService.STAFF.contains(r)) {
                throw new InvalidRequestException("role", "must be OWNER, MANAGER or CASHIER");
            }
        }
        return staff.list(search, r, active);
    }

    @GetMapping("/{id}")
    public StaffMember get(@PathVariable Long id) {
        return staff.get(id);
    }

    @PostMapping
    public ResponseEntity<StaffMember> create(@Valid @RequestBody CreateStaffRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(StaffMember.of(staff.create(body)));
    }

    @PutMapping("/{id}")
    public StaffMember update(@PathVariable Long id, @Valid @RequestBody UpdateStaffRequest body) {
        return staff.update(id, body);
    }

    @PatchMapping("/{id}/status")
    public StaffMember status(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        return staff.setActive(id, body.active());
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest body) {
        staff.resetPassword(id, body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
