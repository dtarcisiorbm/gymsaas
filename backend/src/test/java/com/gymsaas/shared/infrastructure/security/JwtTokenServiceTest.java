package com.gymsaas.shared.infrastructure.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    private SecurityProperties properties;
    private JwtTokenService tokenService;
    private RevokedSessionService revokedSessions;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties(
                new SecurityProperties.Jwt("gymsaas", "gymsaas-api", Duration.ofMinutes(15),
                        Duration.ofDays(30), "", ""),
                new SecurityProperties.Cors(List.of("http://localhost:3000")),
                new SecurityProperties.Cookie(false, "Lax", "gymsaas_refresh_token"));
        JwtConfig config = new JwtConfig();
        RSAKey key = config.jwtRsaKey(properties);
        tokenService = new JwtTokenService(config.jwtEncoder(key), properties);
        revokedSessions = new RevokedSessionService(properties);
        decoder = config.jwtDecoder(key, properties, revokedSessions);
    }

    @Test
    void issuesSignedTokenWithTenantRolesAndPermissions() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        JwtTokenService.IssuedAccessToken issued = tokenService.issueAccessToken(userId,
                "admin@gymsaas.local", tenantId, familyId,
                Set.of("TENANT_ADMIN"), Set.of("user:read", "role:read"), false);

        Jwt jwt = decoder.decode(issued.value());
        assertEquals("gymsaas", jwt.getClaimAsString("iss"));
        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals(tenantId.toString(), jwt.getClaimAsString("tid"));
        assertEquals(familyId.toString(), jwt.getClaimAsString("fid"));
        assertTrue(jwt.getAudience().contains("gymsaas-api"));
        assertEquals(List.of("TENANT_ADMIN"), jwt.getClaimAsStringList("roles"));
        assertTrue(jwt.getClaimAsStringList("perms").contains("user:read"));
        assertEquals(900, issued.expiresInSeconds());
    }

    @Test
    void supportsSessionWithoutSelectedTenant() {
        JwtTokenService.IssuedAccessToken issued = tokenService.issueAccessToken(
                UUID.randomUUID(), "multi@gymsaas.local", null, UUID.randomUUID(),
                Set.of(), Set.of(), false);

        Jwt jwt = decoder.decode(issued.value());
        assertEquals(null, jwt.getClaimAsString("tid"));
    }

    @Test
    void issuesPlatformAdministratorClaimOnlyWhenRequested() {
        JwtTokenService.IssuedAccessToken issued = tokenService.issueAccessToken(
                UUID.randomUUID(), "superadmin@gymsaas.local", null, UUID.randomUUID(),
                Set.of(), Set.of(), true);

        Jwt jwt = decoder.decode(issued.value());
        assertEquals(Boolean.TRUE, jwt.getClaim("plat"));
        assertEquals(null, jwt.getClaimAsString("tid"));
    }

    @Test
    void rejectsRevokedSessionFamily() {
        UUID familyId = UUID.randomUUID();
        JwtTokenService.IssuedAccessToken issued = tokenService.issueAccessToken(
                UUID.randomUUID(), "user@gymsaas.local", UUID.randomUUID(), familyId,
                Set.of("TENANT_ADMIN"), Set.of("user:read"), false);

        decoder.decode(issued.value());
        revokedSessions.revoke(familyId);
        assertThrows(JwtException.class, () -> decoder.decode(issued.value()));
    }
}
