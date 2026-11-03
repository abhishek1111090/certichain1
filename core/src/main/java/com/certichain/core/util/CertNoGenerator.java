package com.certichain.core.util;

import java.security.SecureRandom;
import java.time.Year;

/**
 * Generates non-guessable certificate numbers such as {@code MUJ-2026-7K3QX9PD}.
 * The alphabet leaves out 0/O/1/I so numbers can be read out over the phone.
 */
public final class CertNoGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private CertNoGenerator() {
    }

    public static String next(String institutionCode) {
        StringBuilder sb = new StringBuilder(institutionCode).append('-').append(Year.now()).append('-');
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
