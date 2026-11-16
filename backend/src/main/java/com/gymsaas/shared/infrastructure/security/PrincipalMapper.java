package com.gymsaas.shared.infrastructure.security;

import com.gymsaas.shared.application.CurrentPrincipal;
import com.gymsaas.shared.domain.UuidV7;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/** Maps the verified JWT to the application-level principal. Never trusts headers. */
public final class PrincipalMapper {

    private PrincipalMapper() {
    }

    public static CurrentPrincipal from(JwtAuthenticationToken authentication) {
        return from(authentication.getToken());
    }

    public static CurrentPrincipal from(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID tenantId = UuidV7.parseOrNull(jwt.getClaimAsString("tid"));
        UUID familyId = UuidV7.parseOrNull(jwt.getClaimAsString("fid"));
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> permissions = jwt.getClaimAsStringList("perms");
        Boolean platformAdmin = jwt.getClaim("plat");
        return new CurrentPrincipal(userId, jwt.getClaimAsString("email"), tenantId, familyId,
                roles == null ? java.util.Set.of() : new LinkedHashSet<>(roles),
                permissions == null ? java.util.Set.of() : new LinkedHashSet<>(permissions),
                Boolean.TRUE.equals(platformAdmin));
    }
}
