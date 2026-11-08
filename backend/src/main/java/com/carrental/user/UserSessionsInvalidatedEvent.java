package com.carrental.user;

import java.util.UUID;

/**
 * Published when a user's existing sessions must end (account disabled, password changed).
 * The auth module listens and revokes refresh tokens, so this module does not depend on it.
 */
public record UserSessionsInvalidatedEvent(UUID userId) {
}
