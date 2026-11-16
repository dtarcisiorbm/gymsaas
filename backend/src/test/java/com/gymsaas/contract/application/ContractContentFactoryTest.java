package com.gymsaas.contract.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.membership.domain.ContractedConditions;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.membership.domain.MembershipTerm;
import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.shared.domain.Sha256;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentStatus;
import com.gymsaas.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractContentFactoryTest {

    private final ContractContentFactory factory = new ContractContentFactory();

    @Test
    void rendersContractedFactsAcademyClausesAndLegalNotice() {
        String content = factory.render(data("Cláusula 1: regras de uso definidas pela academia."));

        assertTrue(content.contains("CONTRATO DE MATRÍCULA Nº M2027000007 — VERSÃO 1"), content);
        assertTrue(content.contains("Vigência: 31/01/2027 a 27/02/2027"), content);
        assertTrue(content.matches("(?s).*Valor por ciclo de cobrança: R\\$[\\s\\u00a0]99,90.*"), content);
        assertTrue(content.matches("(?s).*Desconto: R\\$[\\s\\u00a0]10,00 \\(Indicação\\).*"), content);
        assertTrue(content.matches("(?s).*Valor final por ciclo: R\\$[\\s\\u00a0]89,90.*"), content);
        assertTrue(content.contains("***.456.789-**"));
        assertFalse(content.contains("12345678909"), "full CPF must never be rendered");
        assertTrue(content.contains("Cláusula 1: regras de uso definidas pela academia."));
        assertTrue(content.contains("Horário de acesso 06:00–12:00 (Seg, Ter, Qua, Qui, Sex)"), content);
        assertTrue(content.contains("Acesso à unidade Unidade Centro"));
        assertTrue(content.contains("Máximo de congelamentos: 2"));
        assertTrue(content.contains(ContractContentFactory.LEGAL_NOTICE));
    }

    @Test
    void noInventedClausesWhenAcademyDidNotProvideAny() {
        String content = factory.render(data(null));
        assertTrue(content.contains("Nenhuma cláusula adicional cadastrada no plano."));
    }

    @Test
    void sameInputProducesSameHash() {
        ContractContentFactory.ContractData data = data("x");
        assertEquals(Sha256.hex(factory.render(data)), Sha256.hex(factory.render(data)));
    }

    private static ContractContentFactory.ContractData data(String terms) {
        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("planName", "Mensal Livre");
        snapshot.put("planCode", "MENSAL");
        if (terms != null) {
            snapshot.put("contractTerms", terms);
        }
        snapshot.put("features", List.of(
                Map.of("type", "TIME_WINDOW", "startTime", "06:00", "endTime", "12:00", "daysOfWeek", 31),
                Map.of("type", "BRANCH_ACCESS", "branchId", branchId.toString())));
        ContractedConditions conditions = new ContractedConditions(DurationType.MONTHLY, 1, new BigDecimal("99.90"),
                new BigDecimal("0.00"), "BRL", BillingPeriod.MONTHLY, 1, new BigDecimal("10.00"), "Indicação",
                new BigDecimal("89.90"), new FreezePolicy(30, 2, null), snapshot);
        MembershipTerm.Period period = MembershipTerm.first(LocalDate.of(2027, 1, 31), DurationType.MONTHLY, 1);
        Membership membership = new Membership(UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), branchId, "M2027000007", MembershipStatus.ACTIVE, period.startDate(),
                period.endDate(), period.endDate(), period.anchorDate(), 1, conditions, null, null, null, null, null,
                null, 0, Instant.EPOCH, Instant.EPOCH);
        Tenant tenant = new Tenant(tenantId, "Academia Demo", "Academia Demo Ltda", "12345678000199", null, null,
                "ACTIVE", Instant.EPOCH, Instant.EPOCH);
        Branch branch = new Branch(branchId, tenantId, "Unidade Centro", "BR-1", null, null, null, null, null, null,
                null, null, null, null, "ACTIVE", Instant.EPOCH, Instant.EPOCH);
        Student student = new Student(UUID.randomUUID(), tenantId, "202700001", "Maria Silva", null, null, null, null,
                null, null, StudentStatus.ACTIVE, null, 0, Instant.EPOCH, Instant.EPOCH, null);
        return new ContractContentFactory.ContractData(1, Instant.parse("2027-01-31T13:00:00Z"),
                ZoneId.of("America/Sao_Paulo"), tenant, branch, student, "***.456.789-**", membership,
                Map.of(branchId, "Unidade Centro"));
    }
}
