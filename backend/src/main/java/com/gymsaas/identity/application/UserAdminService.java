package com.gymsaas.identity.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.branch.domain.UserBranchAccessRepository;
import com.gymsaas.identity.domain.TenantMember;
import com.gymsaas.identity.domain.User;
import com.gymsaas.identity.domain.UserRepository;
import com.gymsaas.rbac.domain.Role;
import com.gymsaas.rbac.domain.RoleRepository;
import com.gymsaas.rbac.domain.UserRoleRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.domain.Membership;
import com.gymsaas.tenant.domain.MembershipRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final BranchRepository branchRepository;
    private final UserBranchAccessRepository branchAccessRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserAdminService(UserRepository userRepository,
                            MembershipRepository membershipRepository,
                            RoleRepository roleRepository,
                            UserRoleRepository userRoleRepository,
                            BranchRepository branchRepository,
                            UserBranchAccessRepository branchAccessRepository,
                            PasswordEncoder passwordEncoder,
                            AuditService auditService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.branchRepository = branchRepository;
        this.branchAccessRepository = branchAccessRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<UserAdminDtos.UserSummary> list(UUID tenantId, String status, String search,
                                                      int page, int size) {
        PageResult<TenantMember> members = userRepository.findMembersByTenantId(tenantId,
                normalizeStatus(status), search, page, clampSize(size));
        return PageResult.of(members.content().stream().map(this::toSummary).toList(),
                members.totalElements(), members.page(), members.size());
    }

    @Transactional
    public UserAdminDtos.UserSummary create(UUID tenantId, UserAdminDtos.CreateUserRequest request) {
        Set<UUID> roleIds = request.roleIds() == null ? Set.of() : request.roleIds();
        Set<UUID> branchIds = request.branchIds() == null ? Set.of() : request.branchIds();
        ensureRolesAllowed(tenantId, roleIds);
        ensureBranchesAllowed(tenantId, branchIds);

        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIncludingInactive(email).orElse(null);
        boolean created = false;

        if (user == null) {
            Instant now = Instant.now();
            user = new User(UuidV7.generate(), email, passwordEncoder.encode(request.password()),
                    request.fullName().trim(), BrDocuments.phone(request.phone()), "ACTIVE",
                    false, false, null, now);
            user = userRepository.save(user);
            created = true;
        } else if (membershipRepository.findByUserAndTenantIncludingInactive(user.id(), tenantId).isPresent()) {
            throw ApiException.conflict("MEMBERSHIP_EXISTS",
                    "Este usuário já possui vínculo com a academia.");
        }

        boolean firstMembership = membershipRepository.findActiveByUserId(user.id()).isEmpty();
        Membership membership = membershipRepository.create(
                UuidV7.generate(), user.id(), tenantId, firstMembership);

        if (!roleIds.isEmpty()) {
            userRoleRepository.replaceRoles(membership.id(), tenantId, roleIds, TenantContext.userId());
        }
        if (!branchIds.isEmpty()) {
            branchAccessRepository.replaceAccess(tenantId, membership.id(), branchIds,
                    TenantContext.userId());
        }

        auditService.recordForTenant(tenantId, created ? "USER_CREATED" : "USER_ATTACHED",
                "user", user.id(), "MEDIUM", null,
                Map.of("email", user.email(),
                        "roles", roleIds.stream().map(UUID::toString).toList(),
                        "branches", branchIds.stream().map(UUID::toString).toList()),
                Map.of());

        return summary(user, membership);
    }

    @Transactional
    public UserAdminDtos.UserSummary update(UUID tenantId, UUID userId,
                                            UserAdminDtos.UpdateUserRequest request) {
        Membership membership = requireMembership(tenantId, userId);
        User user = requireUser(userId);

        User updated = new User(user.id(), user.email(), user.passwordHash(),
                request.fullName().trim(), BrDocuments.phone(request.phone()), user.status(),
                user.mfaEnabled(), user.platformAdmin(), user.lastLoginAt(), user.createdAt());
        updated = userRepository.save(updated);

        auditService.recordChange("USER_UPDATED", "user", userId, "MEDIUM",
                Map.of("fullName", user.fullName(), "phone", String.valueOf(user.phone())),
                Map.of("fullName", updated.fullName(), "phone", String.valueOf(updated.phone())));

        return summary(updated, membership);
    }

    @Transactional
    public UserAdminDtos.UserSummary assignRoles(UUID tenantId, UUID userId,
                                                 UserAdminDtos.AssignRolesRequest request) {
        Membership membership = requireMembership(tenantId, userId);
        Set<UUID> roleIds = request.roleIds() == null ? Set.of() : request.roleIds();
        ensureRolesAllowed(tenantId, roleIds);

        List<UUID> previousRoleIds = userRoleRepository.findRoleIds(membership.id());
        Set<String> previousCodes = roleCodes(previousRoleIds);

        userRoleRepository.replaceRoles(membership.id(), tenantId, roleIds, TenantContext.userId());

        Set<String> newCodes = roleCodes(roleIds);
        auditService.recordChange("USER_ROLES_UPDATED", "user", userId, "HIGH",
                Map.of("roles", previousCodes), Map.of("roles", newCodes));

        return summary(requireUser(userId), membership);
    }

    @Transactional
    public UserAdminDtos.UserSummary updateStatus(UUID tenantId, UUID userId,
                                                  UserAdminDtos.UpdateStatusRequest request) {
        Membership membership = requireMembership(tenantId, userId);
        User user = requireUser(userId);
        String previousStatus = user.status();

        User updated = new User(user.id(), user.email(), user.passwordHash(), user.fullName(),
                user.phone(), request.status(), user.mfaEnabled(), user.platformAdmin(),
                user.lastLoginAt(), user.createdAt());
        updated = userRepository.save(updated);

        auditService.recordChange("USER_STATUS_UPDATED", "user", userId, "HIGH",
                Map.of("status", previousStatus), Map.of("status", updated.status()));

        return summary(updated, membership);
    }

    @Transactional(readOnly = true)
    public List<UserAdminDtos.UserBranchView> branchAccess(UUID tenantId, UUID userId) {
        Membership membership = requireMembership(tenantId, userId);
        List<UUID> branchIds = branchAccessRepository.findBranchIds(tenantId, membership.id());
        if (branchIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, Branch> branches = branchRepository.findAllByIds(tenantId, branchIds).stream()
                .collect(Collectors.toMap(Branch::id, branch -> branch));
        return branchIds.stream()
                .map(branches::get)
                .filter(java.util.Objects::nonNull)
                .map(branch -> new UserAdminDtos.UserBranchView(branch.id(), branch.name(),
                        branch.code(), branch.status()))
                .toList();
    }

    @Transactional
    public List<UserAdminDtos.UserBranchView> replaceBranches(UUID tenantId, UUID userId,
                                                              UserAdminDtos.AssignBranchesRequest request) {
        Membership membership = requireMembership(tenantId, userId);
        Set<UUID> branchIds = request.branchIds() == null ? Set.of() : request.branchIds();
        ensureBranchesAllowed(tenantId, branchIds);

        List<UUID> previous = branchAccessRepository.findBranchIds(tenantId, membership.id());
        branchAccessRepository.replaceAccess(tenantId, membership.id(), branchIds,
                TenantContext.userId());

        auditService.recordChange("USER_BRANCHES_UPDATED", "user", userId, "HIGH",
                Map.of("branches", previous.stream().map(UUID::toString).toList()),
                Map.of("branches", branchIds.stream().map(UUID::toString).toList()));

        return branchAccess(tenantId, userId);
    }

    @Transactional
    public List<UserAdminDtos.UserBranchView> assignBranch(UUID tenantId, UUID userId, UUID branchId) {
        Membership membership = requireMembership(tenantId, userId);
        ensureBranchesAllowed(tenantId, Set.of(branchId));
        Set<UUID> branchIds = new LinkedHashSet<>(
                branchAccessRepository.findBranchIds(tenantId, membership.id()));
        branchIds.add(branchId);
        branchAccessRepository.replaceAccess(tenantId, membership.id(), branchIds,
                TenantContext.userId());
        auditService.recordForTenant(tenantId, "USER_BRANCH_ASSIGNED", "user", userId, "MEDIUM",
                null, Map.of("branch", branchId.toString()), Map.of());
        return branchAccess(tenantId, userId);
    }

    @Transactional
    public List<UserAdminDtos.UserBranchView> removeBranch(UUID tenantId, UUID userId, UUID branchId) {
        Membership membership = requireMembership(tenantId, userId);
        Set<UUID> branchIds = new LinkedHashSet<>(
                branchAccessRepository.findBranchIds(tenantId, membership.id()));
        if (!branchIds.remove(branchId)) {
            throw ApiException.notFound("BRANCH_ACCESS_NOT_FOUND",
                    "Usuário não possui acesso a esta unidade.");
        }
        branchAccessRepository.replaceAccess(tenantId, membership.id(), branchIds,
                TenantContext.userId());
        auditService.recordForTenant(tenantId, "USER_BRANCH_REMOVED", "user", userId, "MEDIUM",
                Map.of("branch", branchId.toString()), null, Map.of());
        return branchAccess(tenantId, userId);
    }

    private Membership requireMembership(UUID tenantId, UUID userId) {
        return membershipRepository.findActiveByUserAndTenant(userId, tenantId)
                .orElseThrow(() -> ApiException.notFound("MEMBERSHIP_NOT_FOUND",
                        "Usuário não possui vínculo ativo com a academia."));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Usuário não encontrado."));
    }

    private void ensureRolesAllowed(UUID tenantId, Set<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        Set<UUID> allowed = roleRepository.findAssignableForTenant(tenantId).stream()
                .map(Role::id)
                .collect(Collectors.toSet());
        if (!allowed.containsAll(roleIds)) {
            throw ApiException.badRequest("ROLE_NOT_ALLOWED",
                    "Um ou mais papéis não pertencem a esta academia.");
        }
    }

    private void ensureBranchesAllowed(UUID tenantId, Set<UUID> branchIds) {
        if (branchIds.isEmpty()) {
            return;
        }
        List<Branch> branches = branchRepository.findAllByIds(tenantId, branchIds);
        if (branches.size() != branchIds.size()) {
            throw ApiException.badRequest("BRANCH_NOT_ALLOWED",
                    "Uma ou mais unidades não pertencem a esta academia.");
        }
        if (branches.stream().anyMatch(branch -> !branch.active())) {
            throw ApiException.badRequest("BRANCH_INACTIVE",
                    "Não é possível vincular usuários a uma unidade inativa.");
        }
    }

    private UserAdminDtos.UserSummary summary(User user, Membership membership) {
        return new UserAdminDtos.UserSummary(user.id(), user.email(), user.fullName(), user.phone(),
                user.status(), membership.id(), membership.defaultMembership(),
                roleCodes(membership.id()),
                branchAccessRepository.findBranchIds(membership.tenantId(), membership.id()));
    }

    private UserAdminDtos.UserSummary toSummary(TenantMember member) {
        User user = requireUser(member.userId());
        return new UserAdminDtos.UserSummary(member.userId(), member.email(), member.fullName(),
                user.phone(), member.status(), member.membershipId(), member.defaultMembership(),
                roleCodes(member.membershipId()),
                branchAccessRepository.findBranchIds(TenantContext.requireTenantId(),
                        member.membershipId()));
    }

    private Set<String> roleCodes(UUID membershipId) {
        return roleCodes(userRoleRepository.findRoleIds(membershipId));
    }

    private Set<String> roleCodes(java.util.Collection<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return roleRepository.findByIds(roleIds).stream()
                .map(Role::code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(normalized)) {
            throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
        return normalized;
    }

    private int clampSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }
}
