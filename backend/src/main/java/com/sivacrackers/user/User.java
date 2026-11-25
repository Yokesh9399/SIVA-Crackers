package com.sivacrackers.user;

import com.sivacrackers.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A customer or staff account.
 *
 * <p>SECURITY: only a BCrypt hash is stored (the database rejects anything that is not a
 * BCrypt hash). {@link #toString()} never includes the hash.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Stored lower-case; uniqueness is also case-insensitive in the database. */
    @Column(nullable = false, length = 254)
    private String email;

    /** Optional 10-digit Indian mobile number. */
    @Column(length = 15)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Included in every session token; increasing it invalidates all of this user's sessions. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    protected User() {
        // for JPA
    }

    public User(String fullName, String email, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public int getTokenVersion() { return tokenVersion; }

    /** Ends every existing session of this user (e.g. after a password change). */
    public void invalidateSessions() { this.tokenVersion++; }

    @Override
    public String toString() {
        return "User[id=" + getId() + ", role=" + role + ", active=" + active + "]";
    }
}
