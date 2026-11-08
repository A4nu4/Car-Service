package com.carrental.booking;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Human-friendly booking codes like "BK-7KQ2M9XA" for emails and the rental desk. Ambiguous
 * characters (0/O, 1/I) are excluded. 32^8 combinations make collisions practically impossible,
 * and the unique constraint catches the rest.
 */
@Component
public class BookingReferenceGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder reference = new StringBuilder("BK-");
        for (int i = 0; i < LENGTH; i++) {
            reference.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return reference.toString();
    }
}
