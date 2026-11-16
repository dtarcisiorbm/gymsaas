package com.gymsaas.shared.infrastructure.bootstrap;

import com.gymsaas.branch.infrastructure.persistence.BranchJpaEntity;
import com.gymsaas.branch.infrastructure.persistence.BranchJpaRepository;
import com.gymsaas.branch.infrastructure.persistence.BranchSettingsJpaEntity;
import com.gymsaas.branch.infrastructure.persistence.BranchSettingsJpaRepository;
import com.gymsaas.branch.infrastructure.persistence.UserBranchAccessJpaEntity;
import com.gymsaas.branch.infrastructure.persistence.UserBranchAccessJpaRepository;
import com.gymsaas.identity.infrastructure.persistence.UserJpaEntity;
import com.gymsaas.identity.infrastructure.persistence.UserJpaRepository;
import com.gymsaas.rbac.infrastructure.persistence.RoleJpaEntity;
import com.gymsaas.rbac.infrastructure.persistence.RoleJpaRepository;
import com.gymsaas.rbac.infrastructure.persistence.UserRoleJpaEntity;
import com.gymsaas.rbac.infrastructure.persistence.UserRoleJpaRepository;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.infrastructure.persistence.TenantJpaEntity;
import com.gymsaas.tenant.infrastructure.persistence.TenantJpaRepository;
import com.gymsaas.tenant.infrastructure.persistence.UserTenantJpaEntity;
import com.gymsaas.tenant.infrastructure.persistence.UserTenantJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provisions a demo academy and users so the UI can be exercised locally.
 * Disabled by default outside the demo profile. Idempotent: safe to run on
 * every boot. Runs with RLS bypass because no tenant session exists yet.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_DOCUMENT = "12345678000199";
    private static final String DEMO_TENANT_NAME = "Academia Demo";
    private static final String DEMO_BRANCH_CODE = "BR-DEMO";

    private final DemoDataProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final TenantJpaRepository tenants;
    private final UserJpaRepository users;
    private final UserTenantJpaRepository memberships;
    private final RoleJpaRepository roles;
    private final UserRoleJpaRepository userRoles;
    private final BranchJpaRepository branches;
    private final BranchSettingsJpaRepository branchSettings;
    private final UserBranchAccessJpaRepository branchAccess;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(DemoDataProperties properties,
                          PlatformTransactionManager transactionManager,
                          TenantJpaRepository tenants,
                          UserJpaRepository users,
                          UserTenantJpaRepository memberships,
                          RoleJpaRepository roles,
                          UserRoleJpaRepository userRoles,
                          BranchJpaRepository branches,
                          BranchSettingsJpaRepository branchSettings,
                          UserBranchAccessJpaRepository branchAccess,
                          PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.tenants = tenants;
        this.users = users;
        this.memberships = memberships;
        this.roles = roles;
        this.userRoles = userRoles;
        this.branches = branches;
        this.branchSettings = branchSettings;
        this.branchAccess = branchAccess;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        TenantContext.runWithBypass(() -> transactionTemplate.executeWithoutResult(status -> seed()));
    }

    private void seed() {
        Instant now = Instant.now();
        TenantJpaEntity tenant = tenants.findByCnpj(DEMO_DOCUMENT).orElseGet(() -> tenants.save(
                new TenantJpaEntity(UuidV7.generate(), DEMO_TENANT_NAME, "Academia Demo Ltda",
                        DEMO_DOCUMENT, "contato@gymsaas.local", "(11) 4000-0000", "ACTIVE",
                        now, now)));

        BranchJpaEntity branch = ensureBranch(tenant.getId(), now);

        UserJpaEntity superAdmin = ensureUser("superadmin@gymsaas.local", "SuperAdmin@123",
                "Administrador da Plataforma", true, null);
        UserJpaEntity admin = ensureUser("admin@gymsaas.local", "Admin@123",
                "Administrador Demo", false, "(11) 90000-0001");
        UserJpaEntity reception = ensureUser("reception@gymsaas.local", "Reception@123",
                "Recepcionista Demo", false, "(11) 90000-0002");

        UserTenantJpaEntity adminMembership = ensureMembership(admin.getId(), tenant.getId(), true);
        UserTenantJpaEntity receptionMembership = ensureMembership(reception.getId(), tenant.getId(), false);

        assignSystemRole(adminMembership, "TENANT_ADMIN");
        assignSystemRole(receptionMembership, "RECEPTIONIST");
        ensureBranchAccess(tenant.getId(), adminMembership.getId(), branch.getId());

        log.info("Demo data ready: tenant='{}' branch='{}' users=[superadmin@gymsaas.local, "
                        + "admin@gymsaas.local, reception@gymsaas.local]",
                tenant.getName(), branch.getName());
    }

    private BranchJpaEntity ensureBranch(UUID tenantId, Instant now) {
        return branches.findByTenantIdAndCodeIgnoreCase(tenantId, DEMO_BRANCH_CODE)
                .orElseGet(() -> {
                    BranchJpaEntity branch = branches.save(new BranchJpaEntity(UuidV7.generate(),
                            tenantId, "Unidade Centro", DEMO_BRANCH_CODE, DEMO_DOCUMENT,
                            "centro@gymsaas.local", "(11) 4000-0001", "01001000",
                            "Praça da Sé", "1", "Térreo", "Sé", "São Paulo", "SP", "ACTIVE",
                            now, now));
                    branchSettings.save(new BranchSettingsJpaEntity(UuidV7.generate(), tenantId,
                            branch.getId(), "America/Sao_Paulo", null, null, 300, false, Map.of()));
                    return branch;
                });
    }

    private UserJpaEntity ensureUser(String email, String rawPassword, String fullName,
                                     boolean platformAdmin, String phone) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            Instant now = Instant.now();
            return users.save(new UserJpaEntity(UuidV7.generate(), email,
                    passwordEncoder.encode(rawPassword), fullName, phone, "ACTIVE", false,
                    platformAdmin, null, now));
        });
    }

    private UserTenantJpaEntity ensureMembership(UUID userId, UUID tenantId, boolean isDefault) {
        return memberships.findByUserIdAndTenantId(userId, tenantId).orElseGet(() ->
                memberships.save(new UserTenantJpaEntity(UuidV7.generate(), userId, tenantId,
                        isDefault, "ACTIVE")));
    }

    private void ensureBranchAccess(UUID tenantId, UUID membershipId, UUID branchId) {
        boolean exists = branchAccess.findByTenantIdAndUserTenantId(tenantId, membershipId).stream()
                .anyMatch(access -> access.getBranchId().equals(branchId));
        if (!exists) {
            branchAccess.save(new UserBranchAccessJpaEntity(UuidV7.generate(), tenantId,
                    membershipId, branchId, null, Instant.now()));
        }
    }

    private void assignSystemRole(UserTenantJpaEntity membership, String roleCode) {
        RoleJpaEntity role = roles.findByCodeAndTenantIdIsNull(roleCode).orElse(null);
        if (role == null) {
            log.warn("System role {} not found; skipping demo assignment", roleCode);
            return;
        }
        List<UserRoleJpaEntity> current = userRoles.findByUserTenantId(membership.getId());
        boolean alreadyAssigned = current.stream().anyMatch(link -> link.getRoleId().equals(role.getId()));
        if (!alreadyAssigned) {
            userRoles.save(new UserRoleJpaEntity(UuidV7.generate(), membership.getId(),
                    membership.getTenantId(), role.getId(), null, Instant.now()));
        }
    }
}
