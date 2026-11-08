package com.carrental.user;

import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Locale;

/** Composable, optional filters for user searches. A null argument means "no filter". */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRoleIn(Collection<Role> roles) {
        return (root, query, cb) -> roles == null || roles.isEmpty() ? null : root.get("role").in(roles);
    }

    /** Case-insensitive match on email, first name or last name. */
    public static Specification<User> matchesText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern));
        };
    }
}
