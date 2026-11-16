package com.gymsaas.shared.domain;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * RFC 9562 UUID v7 generator: 48-bit Unix epoch milliseconds followed by random
 * bits. Time-ordered identifiers keep B-tree inserts sequential without leaking
 * record counts (unlike auto-increment).
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        long timestamp = System.currentTimeMillis();
        long randomA = RANDOM.nextInt(1 << 12);
        long msb = ((timestamp & 0xFFFF_FFFF_FFFFL) << 16) | 0x7000L | randomA;
        long lsb = (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }

    public static UUID parseOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
