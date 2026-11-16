package com.gymsaas.identity.application;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCodecTest {

    @Test
    void generatesUrlSafeTokensWith256BitsOfEntropy() {
        String token = RefreshTokenCodec.newToken();
        assertEquals(43, token.length());
        assertTrue(token.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void generatesUniqueTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            tokens.add(RefreshTokenCodec.newToken());
        }
        assertEquals(1_000, tokens.size());
    }

    @Test
    void hashesDeterministicallyAndNeverStoresTheRawValue() {
        String token = RefreshTokenCodec.newToken();
        String hash = RefreshTokenCodec.hash(token);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(hash, RefreshTokenCodec.hash(token));
        assertFalse(hash.contains(token));
        assertNotEquals(hash, RefreshTokenCodec.hash(RefreshTokenCodec.newToken()));
    }
}
