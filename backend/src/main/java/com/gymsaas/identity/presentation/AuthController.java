package com.gymsaas.identity.presentation;

import com.gymsaas.identity.application.AuthDtos;
import com.gymsaas.identity.application.AuthService;
import com.gymsaas.shared.application.CurrentPrincipal;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.infrastructure.security.PrincipalMapper;
import com.gymsaas.shared.infrastructure.security.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final SecurityProperties properties;

    public AuthController(AuthService authService, SecurityProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.SessionResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                          HttpServletRequest http,
                                                          HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request, http.getRemoteAddr(),
                http.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(result, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.SessionResponse> refresh(HttpServletRequest http,
                                                            HttpServletResponse response) {
        String rawToken = readRefreshCookie(http);
        if (rawToken == null) {
            throw ApiException.unauthorized("REFRESH_REQUIRED", "Sessão não encontrada.");
        }
        AuthService.AuthResult result = authService.refresh(rawToken, http.getRemoteAddr(),
                http.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(result, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest http, HttpServletResponse response) {
        authService.logout(readRefreshCookie(http));
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthDtos.MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        CurrentPrincipal principal = PrincipalMapper.from(jwt);
        return authService.me(principal.userId(), principal.tenantId(),
                principal.roles(), principal.permissions());
    }

    @GetMapping("/tenants")
    public List<AuthDtos.TenantView> tenants(@AuthenticationPrincipal Jwt jwt) {
        CurrentPrincipal principal = PrincipalMapper.from(jwt);
        return authService.listTenants(principal.userId());
    }

    @PostMapping("/select-tenant")
    public AuthDtos.TenantSelectionResponse selectTenant(
            @Valid @RequestBody AuthDtos.SelectTenantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        CurrentPrincipal principal = PrincipalMapper.from(jwt);
        return authService.selectTenant(principal.userId(), principal.familyId(), request.tenantId());
    }

    private ResponseEntity<AuthDtos.SessionResponse> withRefreshCookie(AuthService.AuthResult result,
                                                                      HttpServletResponse response) {
        long maxAge = properties.jwt().refreshTokenTtl().toSeconds();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(result.refreshToken(), Duration.ofSeconds(maxAge)));
        return ResponseEntity.status(HttpStatus.OK).body(result.session());
    }

    private String buildCookie(String value, Duration maxAge) {
        SecurityProperties.Cookie cookie = properties.cookie();
        return ResponseCookie.from(cookie.name(), value)
                .httpOnly(true)
                .secure(cookie.secure())
                .sameSite(cookie.sameSite())
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private String readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String name = properties.cookie().name();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
