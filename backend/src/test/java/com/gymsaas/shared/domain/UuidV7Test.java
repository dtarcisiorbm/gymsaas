package com.gymsaas.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidV7Test {

    @Test
    void generatesRfc9562VersionAndVariant() {
        UUID id = UuidV7.generate();
        assertEquals(7, id.version());
        assertEquals(2, id.variant());
    }

    @Test
    void embedsCurrentTimestampInTheFirst48Bits() {
        long before = System.currentTimeMillis();
        UUID id = UuidV7.generate();
        long after = System.currentTimeMillis();
        long timestamp = id.getMostSignificantBits() >>> 16;
        assertTrue(timestamp >= before - 1 && timestamp <= after + 1,
                "timestamp bits should reflect generation time");
    }

    @Test
    void generatesDistinctIdentifiers() {
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(UuidV7.generate());
        }
        assertEquals(10_000, ids.size());
    }

    @Test
    void parsesNullAndBlankSafely() {
        assertNull(UuidV7.parseOrNull(null));
        assertNull(UuidV7.parseOrNull(" "));
        UUID id = UuidV7.generate();
        assertEquals(id, UuidV7.parseOrNull(id.toString()));
    }
}
