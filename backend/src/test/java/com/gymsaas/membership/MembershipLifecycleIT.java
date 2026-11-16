package com.gymsaas.membership;

import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.usecase.CreateMembershipUseCase;
import com.gymsaas.membership.application.usecase.FindMembershipByIdUseCase;
import com.gymsaas.membership.application.usecase.FreezeMembershipUseCase;
import com.gymsaas.membership.application.usecase.GetMembershipHistoryUseCase;
import com.gymsaas.membership.application.usecase.ProcessMembershipLifecycleUseCase;
import com.gymsaas.membership.application.usecase.RenewMembershipUseCase;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.support.Phase5IntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Date-driven lifecycle, run on an isolated tenant so simulated future dates
 * never touch memberships created by other test classes.
 */
class MembershipLifecycleIT extends Phase5IntegrationSupport {

    @Autowired CreateMembershipUseCase create;
    @Autowired RenewMembershipUseCase renew;
    @Autowired FreezeMembershipUseCase freeze;
    @Autowired FindMembershipByIdUseCase find;
    @Autowired GetMembershipHistoryUseCase history;
    @Autowired ProcessMembershipLifecycleUseCase lifecycle;

    record Fixture(UUID tenantId, UUID branchId, UUID studentId, UUID planId) {
    }

    @Test
    void expiresMembershipAndActivatesRenewalIdempotently() {
        Fixture f = fixture();
        MembershipDtos.MembershipDetail membership = asTenant(f.tenantId(), () -> create.execute(f.tenantId(),
                new MembershipDtos.CreateMembershipRequest(f.studentId(), f.planId(), f.branchId(), null, null, null,
                        null), false));
        MembershipDtos.MembershipDetail renewal = asTenant(f.tenantId(), () -> renew.execute(f.tenantId(),
                membership.id(), new MembershipDtos.RenewMembershipRequest(null, null, null, null, null), false));
        assertEquals("PENDING", renewal.status());

        LocalDate dayAfterEnd = membership.endDate().plusDays(1);
        MembershipDtos.LifecycleResult first = asTenant(f.tenantId(),
                () -> lifecycle.execute(f.tenantId(), dayAfterEnd));
        assertEquals(1, first.expired());
        assertEquals(1, first.activated());

        MembershipDtos.LifecycleResult second = asTenant(f.tenantId(),
                () -> lifecycle.execute(f.tenantId(), dayAfterEnd));
        assertEquals(new MembershipDtos.LifecycleResult(0, 0, 0), second);

        assertEquals("EXPIRED", asTenant(f.tenantId(), () -> find.execute(f.tenantId(), membership.id())).status());
        assertEquals("ACTIVE", asTenant(f.tenantId(), () -> find.execute(f.tenantId(), renewal.id())).status());
        MembershipDtos.HistoryEntryView expired = asTenant(f.tenantId(),
                () -> history.execute(f.tenantId(), membership.id())).stream()
                .filter(entry -> entry.type().equals("EXPIRED")).findFirst().orElseThrow();
        assertNull(expired.changedBy(), "system transition has no user");
    }

    @Test
    void freezeEndsAutomaticallyAndKeepsExtendedEndDate() {
        Fixture f = fixture();
        MembershipDtos.MembershipDetail membership = asTenant(f.tenantId(), () -> create.execute(f.tenantId(),
                new MembershipDtos.CreateMembershipRequest(f.studentId(), f.planId(), f.branchId(), null, null, null,
                        null), false));
        MembershipDtos.MembershipDetail frozen = asTenant(f.tenantId(), () -> freeze.execute(f.tenantId(),
                membership.id(), new MembershipDtos.FreezeMembershipRequest(today().plusDays(2), "Viagem")));
        assertEquals("FROZEN", frozen.status());
        assertEquals(membership.endDate().plusDays(3), frozen.endDate());

        MembershipDtos.LifecycleResult result = asTenant(f.tenantId(),
                () -> lifecycle.execute(f.tenantId(), today().plusDays(3)));
        assertEquals(1, result.unfrozen());
        assertEquals(0, result.expired());

        MembershipDtos.MembershipDetail after = asTenant(f.tenantId(), () -> find.execute(f.tenantId(), membership.id()));
        assertEquals("ACTIVE", after.status());
        assertEquals(membership.endDate().plusDays(3), after.endDate());
        assertEquals("COMPLETED", after.freezes().get(0).status());
        assertEquals(3, after.freezes().get(0).effectiveDays());
        assertEquals(3, after.frozenDaysUsed());
    }

    @Test
    void lifecycleOnlySeesItsOwnTenant() {
        Fixture f = fixture();
        Fixture other = fixture();
        MembershipDtos.MembershipDetail membership = asTenant(other.tenantId(), () -> create.execute(other.tenantId(),
                new MembershipDtos.CreateMembershipRequest(other.studentId(), other.planId(), other.branchId(), null,
                        null, null, null), false));
        MembershipDtos.LifecycleResult result = asTenant(f.tenantId(),
                () -> lifecycle.execute(f.tenantId(), membership.endDate().plusDays(1)));
        assertEquals(0, result.expired());
        assertTrue(asTenant(other.tenantId(), () -> find.execute(other.tenantId(), membership.id()))
                .status().equals("ACTIVE"));
    }

    private Fixture fixture() {
        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        String document = tenantId.toString().replace("-", "").substring(0, 14);
        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into tenants (id, name, legal_name, document, cnpj, email, status)
                    values (?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """, tenantId, "Academia Ciclo " + tenantId, "Academia Ciclo Ltda", document, document,
                    tenantId + "@example.com");
            jdbcTemplate.update("insert into branches (id, tenant_id, name, code) values (?, ?, 'Unidade', 'UN1')",
                    branchId, tenantId);
            jdbcTemplate.update("""
                    insert into students (id, tenant_id, registration_number, name, status)
                    values (?, ?, '202700001', 'Aluno Ciclo', 'ACTIVE')
                    """, studentId, tenantId);
            jdbcTemplate.update("""
                    insert into plans (id, tenant_id, code, name, duration_type, duration_value)
                    values (?, ?, 'MENSAL', 'Mensal', 'MONTHLY', 1)
                    """, planId, tenantId);
            jdbcTemplate.update("""
                    insert into plan_prices (tenant_id, plan_id, amount, billing_period, valid_from)
                    values (?, ?, 90.00, 'MONTHLY', ?)
                    """, tenantId, planId, today().minusDays(400));
        });
        return new Fixture(tenantId, branchId, studentId, planId);
    }
}
