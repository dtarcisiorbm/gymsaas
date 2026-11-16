package com.gymsaas.membership;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymsaas.membership.domain.MembershipTerm;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.support.Phase5IntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MembershipManagementIT extends Phase5IntegrationSupport {

    @Test
    void contractingUsesBackendPriceAndSnapshotSurvivesPriceChange() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Matrícula " + unique(""));
        JsonNode plan = createPlan(token, "100.00");
        UUID planId = id(plan);

        Map<String, Object> request = membershipPayload(studentId, planId, branchId);
        request.put("amount", "1.00");       // ignored: clients never send prices
        request.put("finalAmount", "1.00");  // ignored
        LocalDate expectedEnd = MembershipTerm.first(today(), DurationType.MONTHLY, 1).endDate();

        JsonNode quote = body(postJson(token, "/api/memberships/quote", request).andExpect(status().isOk()).andReturn());
        assertEquals("100.00", quote.at("/conditions/amount").asText());
        assertEquals("100.00", quote.at("/conditions/finalAmount").asText());
        assertEquals("20.00", quote.at("/conditions/setupFee").asText());
        assertEquals(expectedEnd.toString(), quote.get("endDate").asText());
        assertEquals("ACTIVE", quote.get("initialStatus").asText());

        JsonNode membership = body(postJson(token, "/api/memberships", request)
                .andExpect(status().isCreated()).andReturn());
        assertEquals("ACTIVE", membership.get("status").asText());
        assertEquals("100.00", membership.at("/conditions/finalAmount").asText());
        assertEquals(expectedEnd.toString(), membership.get("endDate").asText());
        assertTrue(membership.get("registrationNumber").asText().matches("M\\d{10}"));
        UUID membershipId = id(membership);

        postJson(token, "/api/plans/" + planId + "/prices", Map.of("amount", "150.00", "billingPeriod", "MONTHLY",
                "validFrom", today().plusDays(1).toString())).andExpect(status().isCreated());
        getJson(token, "/api/memberships/" + membershipId).andExpect(status().isOk())
                .andExpect(jsonPath("$.conditions.amount").value("100.00"))
                .andExpect(jsonPath("$.conditions.finalAmount").value("100.00"));

        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() -> jdbcTemplate.update(
                "update student_memberships set final_amount = 1, amount = 1 where id = ?", membershipId)));

        postJson(token, "/api/memberships", membershipPayload(studentId, planId, branchId))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBERSHIP_OVERLAP"));
    }

    @Test
    void discountRequiresPermissionAndReason() throws Exception {
        String admin = adminToken();
        UUID branchId = demoBranchId(admin);
        UUID planId = id(createPlan(admin, "100.00"));
        UUID studentId = createStudent(admin, "Aluno Desconto " + unique(""));

        Map<String, Object> request = membershipPayload(studentId, planId, branchId);
        request.put("discountAmount", "25.00");
        request.put("discountReason", "Indicação");
        postJson(receptionToken(), "/api/memberships", request)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("DISCOUNT_NOT_ALLOWED"));

        Map<String, Object> noReason = new HashMap<>(request);
        noReason.remove("discountReason");
        postJson(admin, "/api/memberships", noReason)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DISCOUNT_REASON_REQUIRED"));

        Map<String, Object> tooMuch = new HashMap<>(request);
        tooMuch.put("discountAmount", "100.01");
        postJson(admin, "/api/memberships", tooMuch)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DISCOUNT_EXCEEDS_AMOUNT"));

        postJson(admin, "/api/memberships", request).andExpect(status().isCreated())
                .andExpect(jsonPath("$.conditions.discountAmount").value("25.00"))
                .andExpect(jsonPath("$.conditions.finalAmount").value("75.00"))
                .andExpect(jsonPath("$.conditions.discountReason").value("Indicação"));
    }

    @Test
    void freezeSuspendRenewCancelAndHistory() throws Exception {
        String admin = adminToken();
        String reception = receptionToken();
        UUID branchId = demoBranchId(admin);
        UUID planId = id(createPlan(admin, "100.00"));
        UUID studentId = createStudent(admin, "Aluno Jornada " + unique(""));
        JsonNode membership = createMembership(reception, studentId, planId, branchId);
        UUID membershipId = id(membership);
        LocalDate end = LocalDate.parse(membership.get("endDate").asText());
        String base = "/api/memberships/" + membershipId;

        postJson(admin, "/api/plans/" + planId + "/prices", Map.of("amount", "130.00", "billingPeriod", "MONTHLY",
                "validFrom", today().plusDays(1).toString())).andExpect(status().isCreated());

        postJson(reception, base + "/freeze", Map.of("endDate", today().plusDays(9).toString(), "reason", "Viagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"))
                .andExpect(jsonPath("$.endDate").value(end.plusDays(10).toString()))
                .andExpect(jsonPath("$.freezes[0].plannedDays").value(10));
        postJson(reception, base + "/freeze", Map.of("endDate", today().plusDays(9).toString(), "reason", "x"))
                .andExpect(status().isConflict());
        postJson(reception, base + "/unfreeze", Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.endDate").value(end.toString()))
                .andExpect(jsonPath("$.freezes[0].status").value("CANCELLED"));

        postJson(reception, base + "/suspend", Map.of("reason", "Pendência")).andExpect(status().isForbidden());
        postJson(admin, base + "/suspend", Map.of("reason", "Pendência"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        postJson(admin, base + "/reactivate", Map.of("reason", "Regularizado"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        JsonNode renewal = body(postJson(reception, base + "/renew", Map.of()).andExpect(status().isCreated())
                .andReturn());
        assertEquals(end.plusDays(1).toString(), renewal.get("startDate").asText());
        assertEquals("PENDING", renewal.get("status").asText());
        assertEquals(membershipId.toString(), renewal.get("renewedFromId").asText());
        assertEquals("130.00", renewal.at("/conditions/amount").asText(), "renewal uses the current price (D4)");
        assertEquals(2, renewal.get("termSequence").asInt());
        assertEquals(today().toString(), renewal.get("anchorDate").asText());
        postJson(reception, base + "/renew", Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_RENEWED"));

        getJson(admin, base).andExpect(jsonPath("$.renewedById").value(renewal.get("id").asText()))
                .andExpect(jsonPath("$.conditions.amount").value("100.00"));
        postJson(reception, base + "/freeze", Map.of("endDate", today().plusDays(9).toString(), "reason", "x"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FREEZE_BLOCKED_BY_RENEWAL"));

        JsonNode history = body(getJson(admin, base + "/history").andExpect(status().isOk()).andReturn());
        List<String> types = history.findValuesAsText("type");
        assertTrue(types.containsAll(List.of("CREATED", "FROZEN", "UNFROZEN", "SUSPENDED", "REACTIVATED",
                "RENEWED")), types.toString());

        String renewalBase = "/api/memberships/" + renewal.get("id").asText();
        postJson(admin, renewalBase + "/cancel", Map.of()).andExpect(status().isBadRequest());
        postJson(reception, renewalBase + "/cancel", Map.of("reason", "Desistência")).andExpect(status().isForbidden());
        JsonNode cancelled = body(postJson(admin, renewalBase + "/cancel", Map.of("reason", "Desistência"))
                .andExpect(status().isOk()).andReturn());
        assertEquals("CANCELLED", cancelled.get("status").asText());
        assertEquals("Desistência", cancelled.get("cancellationReason").asText());
        assertNotNull(cancelled.get("cancelledAt"));
        assertNotNull(cancelled.get("cancelledBy"));
        postJson(admin, renewalBase + "/cancel", Map.of("reason", "de novo")).andExpect(status().isConflict());

        getJson(admin, "/api/memberships?studentId=" + studentId).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        int version = body(getJson(admin, base).andReturn()).get("version").asInt();
        putJson(admin, base, Map.of("branchId", branchId.toString(), "notes", "Prefere horário da manhã",
                "version", version + 50)).andExpect(status().isConflict());
        putJson(admin, base, Map.of("branchId", branchId.toString(), "notes", "Prefere horário da manhã",
                "version", version)).andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Prefere horário da manhã"));
    }

    @Test
    void eligibilityAndDateRules() throws Exception {
        String admin = adminToken();
        UUID branchId = demoBranchId(admin);
        UUID planId = id(createPlan(admin, "80.00"));

        UUID blocked = createStudent(admin, "Aluno Bloqueado " + unique(""));
        patchJson(admin, "/api/students/" + blocked + "/status", Map.of("status", "BLOCKED", "reason", "teste"))
                .andExpect(status().isOk());
        postJson(admin, "/api/memberships", membershipPayload(blocked, planId, branchId))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("STUDENT_NOT_ELIGIBLE"));

        UUID student = createStudent(admin, "Aluno Datas " + unique(""));
        Map<String, Object> past = membershipPayload(student, planId, branchId);
        past.put("startDate", today().minusDays(60).toString());
        postJson(admin, "/api/memberships", past)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MEMBERSHIP_ALREADY_ENDED"));

        Map<String, Object> future = membershipPayload(student, planId, branchId);
        future.put("startDate", today().plusDays(10).toString());
        postJson(admin, "/api/memberships", future).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        UUID inactivePlan = id(createPlan(admin, "80.00"));
        patchJson(admin, "/api/plans/" + inactivePlan + "/status", Map.of("status", "INACTIVE"))
                .andExpect(status().isOk());
        postJson(admin, "/api/memberships", membershipPayload(student, inactivePlan, branchId))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PLAN_NOT_ACTIVE"));
    }
}
