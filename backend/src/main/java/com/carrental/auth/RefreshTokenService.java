package com.carrental.auth;

import com.carrental.common.exception.ApiException;
import com.carrental.common.exception.ErrorCode;
import com.carrental.config.JwtProperties;
import com.carrental.user.User;
import com.carrental.user.UserSessionsInvalidatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens are random opaque strings (not JWTs) stored server-side, which is what makes
 * logout and "disable this account now" possible. Every refresh rotates the token; presenting an
 * already-used token means it was copied, so all of that user's sessions are revoked.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository tokens;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, JwtProperties jwtProperties, Clock clock) {
        this.tokens = tokens;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now(clock).plus(jwtProperties.refreshTokenTtl());
        tokens.save(new RefreshToken(user, hash(rawToken), expiresAt));
        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    /**
     * Validates and consumes a refresh token, returning the owner's id. Runs in its own transaction that
     * commits even when it throws, so a reuse-triggered revocation is not rolled back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public UUID consume(String rawToken) {
        Instant now = Instant.now(clock);
        RefreshToken token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(RefreshTokenService::invalid);
        User user = token.getUser();
        if (token.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", user.getId());
            tokens.revokeAllForUser(user.getId(), now);
            throw invalid();
        }
        if (token.isExpired(now) || !user.isEnabled()) {
            throw invalid();
        }
        token.revoke(now);
        return user.getId();
    }

    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(Instant.now(clock)));
    }

    @EventListener
    @Transactional
    public void onSessionsInvalidated(UserSessionsInvalidatedEvent event) {
        tokens.revokeAllForUser(event.userId(), Instant.now(clock));
    }

    /** Housekeeping: drop tokens that expired more than a day ago. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        int deleted = tokens.deleteExpiredBefore(Instant.now(clock).minus(Duration.ofDays(1)));
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static ApiException invalid() {
        return new ApiException(ErrorCode.INVALID_REFRESH_TOKEN, "The refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(String value, Instant expiresAt) {
    }
}
