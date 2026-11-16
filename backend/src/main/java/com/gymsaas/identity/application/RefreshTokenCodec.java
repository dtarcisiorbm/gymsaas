package com.gymsaas.identity.application;

import com.gymsaas.shared.application.ApiException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque refresh tokens: 256 bits of randomness, stored only as SHA-256 hashes.
 * (A fast hash is appropriate here because the token itself has full entropy,
 * unlike a user password.)
 */
public final class RefreshTokenCodec {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RefreshTokenCodec() {
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO_ERROR",
                    "Falha ao processar credencial.");
        }
    }
}
