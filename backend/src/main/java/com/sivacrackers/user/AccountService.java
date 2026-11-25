package com.sivacrackers.user;

import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.UnauthorizedException;
import com.sivacrackers.security.LoginAttemptService;
import com.sivacrackers.user.AuthDtos.ChangePasswordRequest;
import com.sivacrackers.user.AuthDtos.UpdateProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The logged-in user's own account: view, update name/phone, change password. */
@Service
public class AccountService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;

    public AccountService(UserRepository users, PasswordEncoder encoder, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public User get(Long userId) {
        return users.findById(userId).filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Please log in to continue."));
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest req) {
        User user = get(userId);
        String phone = Identifiers.phone(req.phone());
        if (phone != null && !phone.equals(user.getPhone()) && users.existsByPhone(phone)) {
            throw new ConflictException("This mobile number is already used by another account.");
        }
        user.setFullName(req.fullName().trim());
        user.setPhone(phone);
        return user;
    }

    /**
     * Requires the current password (rate-limited like logins). On success every existing session
     * of this user ends; the caller issues a fresh session cookie for the current browser.
     */
    @Transactional
    public User changePassword(Long userId, ChangePasswordRequest req) {
        String key = "password-change:" + userId;
        attempts.checkNotLocked(key);
        User user = get(userId);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            attempts.recordFailure(key);
            throw new InvalidRequestException("currentPassword", "is incorrect");
        }
        attempts.recordSuccess(key);
        PasswordPolicy.require("newPassword", req.newPassword());
        if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new InvalidRequestException("newPassword", "must be different from your current password");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.invalidateSessions();
        return user;
    }
}
