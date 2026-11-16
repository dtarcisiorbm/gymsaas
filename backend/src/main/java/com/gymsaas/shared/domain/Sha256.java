package com.gymsaas.shared.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 helper producing lowercase hex digests (document integrity evidence). */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    public static String hex(String content) {
        return hex(content.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isHex(String value) {
        return value != null && value.matches("^[0-9a-f]{64}$");
    }
}
