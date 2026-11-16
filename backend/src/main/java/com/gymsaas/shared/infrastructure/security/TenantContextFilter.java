package com.gymsaas.shared.infrastructure.security;

import com.gymsaas.shared.application.CurrentPrincipal;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Translates the authenticated JWT into the request-scoped TenantContext used
 * by the RLS-aware DataSource. Authentication endpoints run with explicit RLS
 * bypass because the session does not exist yet.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Set<String> BYPASS_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/tenants",
            "/api/auth/select-tenant");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean bypass = BYPASS_PATHS.contains(request.getRequestURI());
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                Jwt jwt = jwtAuthentication.getToken();
                CurrentPrincipal principal = PrincipalMapper.from(jwt);
                TenantContext.set(new TenantContext.Context(
                        UuidV7.parseOrNull(jwt.getClaimAsString("tid")),
                        UuidV7.parseOrNull(jwt.getSubject()),
                        bypass,
                        principal.platformAdmin()));
            } else if (bypass) {
                TenantContext.set(new TenantContext.Context(null, null, true, false));
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
