package com.sivacrackers.user;

import com.sivacrackers.audit.AuditService;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.security.CurrentUser;
import com.sivacrackers.user.AuthDtos.CreateStaffRequest;
import com.sivacrackers.user.StaffDtos.StaffMember;
import com.sivacrackers.user.StaffDtos.UpdateStaffRequest;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff accounts, managed by the OWNER only.
 *
 * <ul>
 *   <li>Only MANAGER and CASHIER accounts can be created or assigned – never another OWNER.</li>
 *   <li>Nobody can change their own role or deactivate themselves; OWNER accounts cannot be deactivated
 *       or re-assigned here (protects against locking the shop out of its own admin).</li>
 *   <li>Deactivating, changing a role or resetting a password ends that person's existing sessions at once.</li>
 *   <li>Every change is written to the activity log (never the password).</li>
 * </ul>
 */
@Service
public class StaffService {

    static final Set<Role> ASSIGNABLE = EnumSet.of(Role.MANAGER, Role.CASHIER);
    static final Set<Role> STAFF = EnumSet.of(Role.OWNER, Role.MANAGER, Role.CASHIER);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public StaffService(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<StaffMember> list(String search, Role role, Boolean active) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return users.findAllByRoleInOrderByRoleAscFullNameAsc(STAFF).stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> active == null || u.isActive() == active)
                .filter(u -> q.isEmpty() || u.getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || u.getEmail().toLowerCase(Locale.ROOT).contains(q) || (u.getPhone() != null && u.getPhone().contains(q)))
                .map(StaffMember::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public StaffMember get(Long id) {
        return StaffMember.of(findStaff(id));
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
            throw new ConflictException("email", "is already used by another account");
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ConflictException("phone", "is already used by another account");
        }
        User user = new User(req.fullName().trim(), email, encoder.encode(req.password()), req.role());
        user.setPhone(phone);
        User saved = users.save(user);
        audit.record("STAFF_CREATED", "STAFF", saved.getId(), saved.getFullName() + " (" + saved.getRole() + ")");
        return saved;
    }

    @Transactional
    public StaffMember update(Long id, UpdateStaffRequest req) {
        User u = findStaff(id);
        boolean self = Objects.equals(id, CurrentUser.idOrNull());
        if (req.role() != u.getRole()) {
            if (self) {
                throw new InvalidRequestException("role", "cannot be changed for your own account");
            }
            if (u.getRole() == Role.OWNER || !ASSIGNABLE.contains(req.role())) {
                throw new InvalidRequestException("role", "must be MANAGER or CASHIER (an OWNER account cannot be re-assigned)");
            }
        }
        String email = Identifiers.email(req.email());
        String phone = Identifiers.phone(req.phone());
        if (users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("email", "is already used by another account");
        }
        if (phone != null && users.existsByPhoneAndIdNot(phone, id)) {
            throw new ConflictException("phone", "is already used by another account");
        }
        Role before = u.getRole();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPhone(phone);
        if (before != req.role()) {
            u.setRole(req.role());
            u.invalidateSessions();
        }
        users.saveAndFlush(u);
        audit.record("STAFF_UPDATED", "STAFF", id,
                before != req.role() ? "role " + before + " → " + req.role() : "details changed");
        return StaffMember.of(u);
    }

    @Transactional
    public StaffMember setActive(Long id, boolean active) {
        User u = findStaff(id);
        if (Objects.equals(id, CurrentUser.idOrNull())) {
            throw new InvalidRequestException("active", "you cannot change the status of your own account");
        }
        if (u.getRole() == Role.OWNER) {
            throw new InvalidRequestException("active", "an OWNER account cannot be deactivated here");
        }
        if (u.isActive() != active) {
            u.setActive(active);
            if (!active) {
                u.invalidateSessions();
            }
            users.saveAndFlush(u);
            audit.record(active ? "STAFF_ACTIVATED" : "STAFF_DEACTIVATED", "STAFF", id, u.getFullName());
        }
        return StaffMember.of(u);
    }

    /** Sets a temporary password for a MANAGER or CASHIER and signs them out everywhere. */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User u = findStaff(id);
        if (u.getRole() == Role.OWNER) {
            throw new InvalidRequestException("newPassword", "the owner changes their own password on the My Account page");
        }
        PasswordPolicy.require("newPassword", newPassword);
        u.setPasswordHash(encoder.encode(newPassword));
        u.invalidateSessions();
        users.saveAndFlush(u);
        audit.record("STAFF_PASSWORD_RESET", "STAFF", id, u.getFullName());
    }

    private User findStaff(Long id) {
        return users.findById(id).filter(u -> STAFF.contains(u.getRole()))
                .orElseThrow(() -> new NotFoundException("Staff member not found."));
    }
}
