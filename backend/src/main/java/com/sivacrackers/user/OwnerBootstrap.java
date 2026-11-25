package com.sivacrackers.user;

import com.sivacrackers.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time creation of the first OWNER account from BOOTSTRAP_OWNER_EMAIL / BOOTSTRAP_OWNER_PASSWORD.
 *
 * <ul>
 *   <li>Does nothing unless both variables are set.</li>
 *   <li>Creates the OWNER only if <b>no OWNER exists</b>; otherwise the variables are ignored (and a
 *       reminder to remove them is logged). It never changes an existing account or password.</li>
 *   <li>The password must meet the normal password rules and is stored only as a BCrypt hash.</li>
 *   <li>Neither the email nor the password is ever written to the log.</li>
 * </ul>
 * There is no default admin account or password anywhere in the application.
 */
@Component
@Order(0)
public class OwnerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerBootstrap.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public OwnerBootstrap(AppProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createOwnerIfNeeded();
    }

    /** @return true if an OWNER was created by this call */
    @Transactional
    public boolean createOwnerIfNeeded() {
        AppProperties.Bootstrap bootstrap = props.bootstrap();
        if (!bootstrap.isConfigured()) {
            return false;
        }
        if (users.existsByRole(Role.OWNER)) {
            log.warn("An OWNER account already exists, so BOOTSTRAP_OWNER_EMAIL / BOOTSTRAP_OWNER_PASSWORD were ignored. "
                    + "Remove both variables from the environment (backend/.env).");
            return false;
        }
        String email = Identifiers.email(bootstrap.ownerEmail());
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 254) {
            throw new IllegalStateException("BOOTSTRAP_OWNER_EMAIL is not a valid email address.");
        }
        if (!PasswordPolicy.isValid(bootstrap.ownerPassword())) {
            throw new IllegalStateException("BOOTSTRAP_OWNER_PASSWORD does not meet the password rules: " + PasswordPolicy.RULES + ".");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("BOOTSTRAP_OWNER_EMAIL is already used by another account. Choose a different email.");
        }
        users.save(new User("Owner", email, encoder.encode(bootstrap.ownerPassword()), Role.OWNER));
        log.info("OWNER account created from the BOOTSTRAP_OWNER_* settings. Log in via the staff login, then REMOVE "
                + "BOOTSTRAP_OWNER_EMAIL and BOOTSTRAP_OWNER_PASSWORD from backend/.env.");
        return true;
    }
}
