package com.sivacrackers.user;

import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.UnauthorizedException;
import com.sivacrackers.security.LoginAttemptService;
import com.sivacrackers.user.AuthDtos.LoginRequest;
import com.sivacrackers.user.AuthDtos.RegisterRequest;
import java.time.Clock;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login.
 *
 * <p>SECURITY
 * <ul>
 *   <li>Public registration always creates a CUSTOMER – there is no way to choose a role.</li>
 *   <li>Customer login accepts only CUSTOMER accounts; staff login only OWNER/MANAGER/CASHIER.</li>
 *   <li>Wrong password, unknown account, wrong login page and deactivated account all give the
 *       same message, and an unknown account still runs a BCrypt check so timing does not reveal it.</li>
 *   <li>5 failures lock the account for 15 minutes; per-IP limits apply (LoginAttemptService).</li>
 *   <li>Passwords are never logged or returned.</li>
 * </ul>
 */
@Service
public class AuthService {

    static final String INVALID_LOGIN = "Incorrect email/phone or password.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;
    private final Clock clock;
    /** Hash of a random value, compared against when the account does not exist (constant-ish timing). */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, LoginAttemptService attempts, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.attempts = attempts;
        this.clock = clock;
        this.dummyHash = encoder.encode("dummy-" + java.util.UUID.randomUUID());
    }

    @Transactional
    public User register(RegisterRequest req, String clientIp) {
        attempts.acquireRegistrationSlot(clientIp);
        PasswordPolicy.require("password", req.password());
        String email = Identifiers.email(req.email());
        String phone = Identifiers.phone(req.phone());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email address already exists. Please log in instead.");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ConflictException("An account with this mobile number already exists. Please log in instead.");
        }
        User user = new User(req.fullName().trim(), email, encoder.encode(req.password()), Role.CUSTOMER);
        user.setPhone(phone);
        user.setLastLoginAt(clock.instant());
        return users.save(user);
    }

    /** @param staffLogin true for the staff login page, false for the customer login page */
    @Transactional
    public User login(LoginRequest req, boolean staffLogin, String clientIp) {
        attempts.acquireLoginSlot(clientIp);
        String identifier = Identifiers.isEmail(req.identifier())
                ? Identifiers.email(req.identifier())
                : Identifiers.phone(req.identifier());
        String accountKey = (staffLogin ? "staff:" : "customer:") + identifier;
        attempts.checkNotLocked(accountKey);

        Optional<User> found = Identifiers.isEmail(req.identifier())
                ? users.findByEmailIgnoreCase(identifier)
                : users.findByPhone(identifier);
        boolean passwordOk = encoder.matches(req.password(), found.map(User::getPasswordHash).orElse(dummyHash));
        boolean allowed = found.isPresent() && passwordOk
                && found.get().isActive()
                && (staffLogin ? found.get().getRole().isStaff() : found.get().getRole() == Role.CUSTOMER);
        if (!allowed) {
            attempts.recordFailure(accountKey);
            throw new UnauthorizedException(INVALID_LOGIN);
        }
        attempts.recordSuccess(accountKey);
        User user = found.get();
        user.setLastLoginAt(clock.instant());
        return user;
    }
}
