package com.gymsaas.shared.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    public RSAKey jwtRsaKey(SecurityProperties properties) {
        SecurityProperties.Jwt jwt = properties.jwt();
        try {
            RSAPublicKey publicKey;
            RSAPrivateKey privateKey;
            if (hasText(jwt.privateKey()) && hasText(jwt.publicKey())) {
                KeyFactory factory = KeyFactory.getInstance("RSA");
                privateKey = (RSAPrivateKey) factory.generatePrivate(
                        new PKCS8EncodedKeySpec(decodePem(jwt.privateKey())));
                publicKey = (RSAPublicKey) factory.generatePublic(
                        new X509EncodedKeySpec(decodePem(jwt.publicKey())));
            } else {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair pair = generator.generateKeyPair();
                publicKey = (RSAPublicKey) pair.getPublic();
                privateKey = (RSAPrivateKey) pair.getPrivate();
            }
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("gymsaas-phase2").build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to configure JWT RSA keys", exception);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey, SecurityProperties properties,
                                 RevokedSessionService revokedSessionService) {
        NimbusJwtDecoder decoder;
        try {
            decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey())
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to configure JWT decoder", exception);
        }

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.jwt().issuer());
        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            if (token.getAudience().contains(properties.jwt().audience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null));
        };
        OAuth2TokenValidator<Jwt> revocationValidator = token -> {
            String familyId = token.getClaimAsString("fid");
            if (familyId != null && revokedSessionService.isRevoked(familyId)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Session revoked", null));
            }
            return OAuth2TokenValidatorResult.success();
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator, audienceValidator, revocationValidator));
        return decoder;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static byte[] decodePem(String pem) {
        String normalized = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }
}
