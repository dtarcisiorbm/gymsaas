package com.gymsaas.shared.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt, Cors cors, Cookie cookie) {

    public record Jwt(
            String issuer,
            String audience,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            String privateKey,
            String publicKey) {
    }

    public record Cors(List<String> allowedOrigins) {
        public List<String> origins() {
            return allowedOrigins == null ? List.of() : allowedOrigins;
        }
    }

    public record Cookie(boolean secure, String sameSite, String name) {
    }
}
