package com.gymsaas.shared.infrastructure.security;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final SecurityProperties properties;

    public JwtTokenService(JwtEncoder encoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedAccessToken issueAccessToken(UUID userId, String email, UUID tenantId,
                                              UUID familyId, Collection<String> roles,
                                              Collection<String> permissions,
                                              boolean platformAdmin) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .audience(List.of(properties.jwt().audience()))
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("fid", familyId.toString())
                .claim("roles", List.copyOf(roles))
                .claim("perms", List.copyOf(permissions));

        if (tenantId != null) {
            claims.claim("tid", tenantId.toString());
        }
        if (platformAdmin) {
            claims.claim("plat", true);
        }

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedAccessToken(token, properties.jwt().accessTokenTtl().toSeconds());
    }

    public record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
