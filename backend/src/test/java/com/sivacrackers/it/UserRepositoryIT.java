package com.sivacrackers.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivacrackers.user.Role;
import com.sivacrackers.user.User;
import com.sivacrackers.user.UserRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** User accounts: BCrypt-only storage, unique email/phone, allowed roles. */
class UserRepositoryIT extends AbstractMySqlIT {

    // Cost 4 keeps tests fast; production uses cost 12 (SecurityConfig).
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Autowired UserRepository users;
    @Autowired DataSource dataSource;

    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
    }

    private User customer(String email) {
        return new User("Test Customer", email, ENCODER.encode("Example-pass-1"), Role.CUSTOMER);
    }

    @Test
    @DisplayName("User saved with a BCrypt hash; found by email ignoring case; toString hides the hash")
    void saveAndFind() {
        User saved = users.saveAndFlush(customer("asha@example.test"));
        User found = users.findByEmailIgnoreCase("ASHA@Example.TEST").orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getPasswordHash()).startsWith("$2a$").hasSize(60);
        assertThat(ENCODER.matches("Example-pass-1", found.getPasswordHash())).isTrue();
        assertThat(found.toString()).doesNotContain(found.getPasswordHash()).doesNotContain("asha");
    }

    @Test
    @DisplayName("Email addresses are unique, case-insensitively")
    void uniqueEmail() {
        users.saveAndFlush(customer("ravi@example.test"));
        assertThatThrownBy(() -> users.saveAndFlush(customer("RAVI@example.test")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Phone numbers are unique and must be 10 digits")
    void phoneRules() {
        User a = customer("a@example.test");
        a.setPhone("9876543210");
        users.saveAndFlush(a);
        User b = customer("b@example.test");
        b.setPhone("9876543210");
        assertThatThrownBy(() -> users.saveAndFlush(b)).isInstanceOf(DataIntegrityViolationException.class);
        User c = customer("c@example.test");
        c.setPhone("12345");
        assertThatThrownBy(() -> users.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("A plain-text (non-BCrypt) password can never be stored")
    void plainTextPasswordRejected() {
        User plain = new User("Plain", "plain@example.test", "my-plain-password", Role.CUSTOMER);
        assertThatThrownBy(() -> users.saveAndFlush(plain))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_users_password_hash");
    }

    @Test
    @DisplayName("Only OWNER, MANAGER, CASHIER and CUSTOMER roles are allowed")
    void onlyKnownRoles() {
        Long id = users.saveAndFlush(customer("role@example.test")).getId();
        assertThatThrownBy(() -> jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_users_role");
    }

    @Test
    @DisplayName("existsByRole supports the one-time OWNER bootstrap")
    void existsByRole() {
        assertThat(users.existsByRole(Role.OWNER)).isFalse();
        users.saveAndFlush(new User("Owner", "owner@example.test", ENCODER.encode("Owner-pass-1"), Role.OWNER));
        assertThat(users.existsByRole(Role.OWNER)).isTrue();
    }
}
