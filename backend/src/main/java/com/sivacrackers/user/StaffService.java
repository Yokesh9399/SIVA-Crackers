package com.sivacrackers.user;

import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.user.AuthDtos.CreateStaffRequest;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff provisioning – the only way MANAGER and CASHIER accounts are created (called by the OWNER).
 * OWNER accounts are never created here (only by the one-time bootstrap).
 */
@Service
public class StaffService {

    private static final Set<Role> ASSIGNABLE = Set.of(Role.MANAGER, Role.CASHIER);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public StaffService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public User create(CreateStaffRequest req) {
        if (!ASSIGNABLE.contains(req.role())) {
            throw new InvalidRequestException("role", "must be MANAGER or CASHIER");
        }
        PasswordPolicy.require("password", req.password());
        String email = Identifiers.email(req.email());
        String phone = Identifiers.phone(req.phone());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email address already exists.");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ConflictException("An account with this mobile number already exists.");
        }
        User user = new User(req.fullName().trim(), email, encoder.encode(req.password()), req.role());
        user.setPhone(phone);
        return users.save(user);
    }
}
