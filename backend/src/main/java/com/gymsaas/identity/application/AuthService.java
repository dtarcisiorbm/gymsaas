package com.gymsaas.identity.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.identity.domain.RefreshToken;
import com.gymsaas.identity.domain.RefreshTokenRepository;
import com.gymsaas.identity.domain.User;
import com.gymsaas.identity.domain.UserRepository;
import com.gymsaas.rbac.domain.Role;
import com.gymsaas.rbac.domain.RoleRepository;
import com.gymsaas.rbac.domain.UserRoleRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.security.JwtTokenService;
import com.gymsaas.shared.infrastructure.security.RevokedSessionService;
import com.gymsaas.shared.infrastructure.security.SecurityProperties;
import com.gymsaas.tenant.domain.Membership;
import com.gymsaas.tenant.domain.MembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RevokedSessionService revokedSessionService;
    private final SecurityProperties properties;
    private final AuditService auditService;
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       MembershipRepository membershipRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder,
                       RevokedSessionService revokedSessionService,
                       SecurityProperties properties,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.revokedSessionService = revokedSessionService;
        this.properties = properties;
        this.auditService = auditService;
        // Equalises timing between unknown e-mail and wrong password.
        this.dummyPasswordHash = passwordEncoder.encode("timing-only-not-a-password");
    }

    @Transactional
    public AuthResult login(AuthDtos.LoginRequest request, String ipAddress, String userAgent) {
        Optional<User> candidate = userRepository.findByEmailIgnoreCase(request.email());
        String hash = candidate.map(User::passwordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (candidate.isEmpty() || !passwordMatches || !candidate.get().active()) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "E-mail ou senha inválidos.");
        }

        User user = candidate.get();
        userRepository.save(user.withLastLogin(Instant.now()));

        List<Membership> memberships = membershipRepository.findActiveByUserId(user.id());
        List<Membership> activeMemberships = memberships.stream()
                .filter(Membership::tenantActive)
                .toList();

        if (memberships.isEmpty() && !user.platformAdmin()) {
            throw ApiException.forbidden("NO_TENANT", "Usuário não possui vínculo ativo com nenhuma academia.");
        }
        if (!memberships.isEmpty() && activeMemberships.isEmpty() && !user.platformAdmin()) {
            throw ApiException.forbidden("TENANT_INACTIVE",
                    "A academia vinculada não está ativa. Contate o administrador.");
        }

        UUID requestedTenant = request.tenantId();
        if (requestedTenant != null && activeMemberships.stream()
                .noneMatch(membership -> membership.tenantId().equals(requestedTenant))) {
            auditService.recordForTenant(requestedTenant, "LOGIN_TENANT_DENIED", "tenant", requestedTenant,
                    "HIGH", null, null, Map.of("reason", "membership not found or tenant inactive"));
            throw ApiException.forbidden("TENANT_NOT_AUTHORIZED", "Acesso não autorizado a esta academia.");
        }

        UUID activeTenantId = requestedTenant;
        if (activeTenantId == null && activeMemberships.size() == 1) {
            activeTenantId = activeMemberships.get(0).tenantId();
        }

        UUID familyId = UuidV7.generate();
        String rawRefreshToken = issueRefreshToken(user.id(), activeTenantId, familyId, ipAddress, userAgent);

        AccessGrant grant = grantFor(user, activeTenantId);
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(
                user.id(), user.email(), activeTenantId, familyId, grant.roles(), grant.permissions(),
                user.platformAdmin());

        List<AuthDtos.TenantView> tenants = activeMemberships.stream().map(this::toTenantView).toList();

        auditService.recordForTenant(activeTenantId, "LOGIN", "user", user.id(), "LOW",
                null, null, Map.of("ip", ipAddress == null ? "" : ipAddress));

        return new AuthResult(new AuthDtos.SessionResponse(
                accessToken.value(), "Bearer", accessToken.expiresInSeconds(),
                toUserView(user), tenants, activeTenantId, user.platformAdmin()), rawRefreshToken);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw ApiException.unauthorized("REFRESH_REQUIRED", "Sessão não encontrada.");
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(RefreshTokenCodec.hash(rawRefreshToken))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH", "Sessão inválida."));

        if (stored.revokedAt() != null) {
            // Token reuse: someone is replaying an already-rotated token. Kill the whole family.
            refreshTokenRepository.revokeFamily(stored.familyId(), "REUSE_DETECTED");
            revokedSessionService.revoke(stored.familyId());
            auditService.recordForTenant(stored.tenantId(), "REFRESH_REUSE_DETECTED", "refresh_token",
                    stored.id(), "CRITICAL", null, null, Map.of("familyId", stored.familyId().toString()));
            log.warn("Refresh token reuse detected for user {} family {}", stored.userId(), stored.familyId());
            throw ApiException.unauthorized("INVALID_REFRESH", "Sessão inválida.");
        }

        Instant now = Instant.now();
        if (!stored.expiresAt().isAfter(now)) {
            refreshTokenRepository.revokeFamily(stored.familyId(), "EXPIRED");
            throw ApiException.unauthorized("REFRESH_EXPIRED", "Sessão expirada.");
        }

        User user = userRepository.findById(stored.userId())
                .filter(User::active)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH", "Sessão inválida."));

        Membership membership = null;
        if (stored.tenantId() != null) {
            membership = membershipRepository.findActiveByUserAndTenant(user.id(), stored.tenantId())
                    .filter(Membership::tenantActive)
                    .orElse(null);
            if (membership == null) {
                refreshTokenRepository.revokeFamily(stored.familyId(), "ADMIN");
                revokedSessionService.revoke(stored.familyId());
                throw ApiException.unauthorized("INVALID_REFRESH", "Sessão inválida.");
            }
        }

        String newRawToken = RefreshTokenCodec.newToken();
        UUID newTokenId = UuidV7.generate();
        Instant expiresAt = now.plus(properties.jwt().refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(newTokenId, user.id(), stored.tenantId(),
                stored.familyId(), RefreshTokenCodec.hash(newRawToken), now, expiresAt,
                null, null, null));
        refreshTokenRepository.save(stored.revoked("ROTATED", now, newTokenId));

        AccessGrant grant = grantFor(user, stored.tenantId());
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(
                user.id(), user.email(), stored.tenantId(), stored.familyId(),
                grant.roles(), grant.permissions(), user.platformAdmin());

        List<AuthDtos.TenantView> tenants = membershipRepository.findActiveByUserId(user.id())
                .stream().filter(Membership::tenantActive).map(this::toTenantView).toList();

        return new AuthResult(new AuthDtos.SessionResponse(
                accessToken.value(), "Bearer", accessToken.expiresInSeconds(),
                toUserView(user), tenants, stored.tenantId(), user.platformAdmin()), newRawToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(RefreshTokenCodec.hash(rawRefreshToken)).ifPresent(token -> {
            refreshTokenRepository.revokeFamily(token.familyId(), "LOGOUT");
            revokedSessionService.revoke(token.familyId());
            auditService.recordForTenant(token.tenantId(), "LOGOUT", "refresh_token", token.id(),
                    "LOW", null, null, Map.of());
        });
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.TenantView> listTenants(UUID userId) {
        return membershipRepository.findActiveByUserId(userId).stream()
                .filter(Membership::tenantActive)
                .map(this::toTenantView)
                .toList();
    }

    @Transactional
    public AuthDtos.TenantSelectionResponse selectTenant(UUID userId, UUID familyId, UUID tenantId) {
        Membership membership = membershipRepository.findActiveByUserAndTenant(userId, tenantId)
                .filter(Membership::tenantActive)
                .orElseThrow(() -> ApiException.forbidden("TENANT_NOT_AUTHORIZED",
                        "Acesso não autorizado a esta academia."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("USER_NOT_FOUND", "Usuário não encontrado."));

        if (familyId != null) {
            refreshTokenRepository.updateTenantForFamily(familyId, tenantId);
        }
        AccessGrant grant = grantFor(user, tenantId);
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(
                userId, user.email(), tenantId, familyId, grant.roles(), grant.permissions(),
                user.platformAdmin());
        auditService.recordForTenant(tenantId, "TENANT_SELECTED", "tenant", tenantId, "LOW",
                null, null, Map.of());

        return new AuthDtos.TenantSelectionResponse(accessToken.value(), "Bearer",
                accessToken.expiresInSeconds(), toTenantView(membership));
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(UUID userId, UUID tenantId, Collection<String> roles,
                                  Collection<String> permissions) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("USER_NOT_FOUND", "Usuário não encontrado."));
        AuthDtos.TenantView tenantView = null;
        if (tenantId != null) {
            tenantView = membershipRepository.findActiveByUserAndTenant(userId, tenantId)
                    .map(this::toTenantView)
                    .orElse(null);
        }
        return new AuthDtos.MeResponse(toUserView(user), tenantView,
                new LinkedHashSet<>(roles), new LinkedHashSet<>(permissions), user.platformAdmin());
    }

    private String issueRefreshToken(UUID userId, UUID tenantId, UUID familyId,
                                     String ipAddress, String userAgent) {
        Instant now = Instant.now();
        String rawToken = RefreshTokenCodec.newToken();
        refreshTokenRepository.save(new RefreshToken(UuidV7.generate(), userId, tenantId, familyId,
                RefreshTokenCodec.hash(rawToken), now, now.plus(properties.jwt().refreshTokenTtl()),
                null, null, null));
        return rawToken;
    }

    /**
     * Platform administrators always receive the SAAS_ADMIN grant on top of any
     * tenant grant, regardless of which academy is active or whether they have
     * a membership at all. Tenant permissions never grant global admin rights.
     */
    private AccessGrant grantFor(User user, UUID tenantId) {
        AccessGrant base = tenantId == null ? AccessGrant.empty()
                : membershipRepository.findActiveByUserAndTenant(user.id(), tenantId)
                        .map(this::loadGrant)
                        .orElseGet(AccessGrant::empty);
        if (!user.platformAdmin()) {
            return base;
        }
        return base.merge(platformGrant());
    }

    private AccessGrant platformGrant() {
        Set<String> permissions = new LinkedHashSet<>(
                roleRepository.findPermissionCodesByRoleCode("SAAS_ADMIN"));
        return new AccessGrant(new LinkedHashSet<>(Set.of("SAAS_ADMIN")), permissions);
    }

    private AccessGrant loadGrant(Membership membership) {
        List<UUID> roleIds = userRoleRepository.findRoleIds(membership.id());
        if (roleIds.isEmpty()) {
            return AccessGrant.empty();
        }
        Set<String> roles = roleRepository.findByIds(roleIds).stream()
                .map(Role::code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissions = new LinkedHashSet<>(
                roleRepository.findPermissionCodesByRoleIds(roleIds));
        return new AccessGrant(roles, permissions);
    }

    private AuthDtos.TenantView toTenantView(Membership membership) {
        Set<String> roles = userRoleRepository.findRoleIds(membership.id()).stream()
                .map(roleId -> roleRepository.findById(roleId).map(Role::code).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new AuthDtos.TenantView(membership.tenantId(), membership.tenantName(),
                membership.defaultMembership(), roles);
    }

    private AuthDtos.UserView toUserView(User user) {
        return new AuthDtos.UserView(user.id(), user.email(), user.fullName(), user.status());
    }

    private String userEmail(UUID userId) {
        return userRepository.findById(userId).map(User::email).orElse("");
    }

    public record AuthResult(AuthDtos.SessionResponse session, String refreshToken) {
    }

    private record AccessGrant(Set<String> roles, Set<String> permissions) {
        static AccessGrant empty() {
            return new AccessGrant(Set.of(), Set.of());
        }

        AccessGrant merge(AccessGrant other) {
            Set<String> mergedRoles = new LinkedHashSet<>(roles);
            mergedRoles.addAll(other.roles());
            Set<String> mergedPermissions = new LinkedHashSet<>(permissions);
            mergedPermissions.addAll(other.permissions());
            return new AccessGrant(mergedRoles, mergedPermissions);
        }
    }
}
