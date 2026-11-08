package com.carrental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credentials for the first administrator account, created on startup when no account with this email exists.
 * Both values are optional; leave them empty once the admin account exists.
 */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String email, String password) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
