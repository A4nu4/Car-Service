package com.carrental.user;

import com.carrental.config.BootstrapAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first ADMIN from environment variables. This avoids shipping a default admin
 * password in a migration, which would be a hardcoded credential in every environment.
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final BootstrapAdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminInitializer(BootstrapAdminProperties properties, UserRepository users,
                                     PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            return;
        }
        String email = User.normalizeEmail(properties.email());
        if (users.existsByEmail(email)) {
            return;
        }
        if (properties.password().length() < MIN_ADMIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD must be at least " + MIN_ADMIN_PASSWORD_LENGTH + " characters");
        }
        users.save(new User(email, passwordEncoder.encode(properties.password()), "System", "Administrator",
                null, Role.ADMIN));
        log.info("Created bootstrap admin account {}", email);
    }
}
