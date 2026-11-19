package com.gymsaas.billing;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Generation from memberships, payments, refunds, cancellation, overdue and summary through the API. */
class BillingManagementIT extends BillingIntegrationSupport {

    @Test
    void newMembershipGeneratesSetupFeeAndInstallmentsRenewalOnlyInstallments() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Cobrança " + unique(""));
        JsonNode plan = createPlan(token, "99.90"); // monthly, setup fee 20.00
        JsonNode membership = createMembership(token, studentId, id(plan), branchId);

        JsonNode generated = membershipCharges(token, id(membership));
        assertEquals(2, generated.size(), generated.toString());
        JsonNode setup = find(generated, "SETUP_FEE");
        JsonNode installment = find(generated, "INSTALLMENT");
        assertEquals("20.00", setup.get("amount").asText());
        assertEquals(membership.get("startDate").asText(), setup.get("dueDate").asText());
        assertEquals("99.90", installment.get("amount").asText());
        assertEquals(1, installment.get("installmentNumber").asInt());
        assertEquals(1, installment.get("installmentCount").asInt());
        assertEquals(membership.get("startDate").asText(), installment.get("periodStart").asText());
        assertEquals(membership.get("originalEndDate").asText(), installment.get("periodEnd").asText());
        assertEquals("OPEN", installment.get("status").asText());
        assertTrue(installment.get("chargeNumber").asText().matches("C\\d{10}"));
        assertEquals(membership.get("registrationNumber").asText(),
                installment.get("membershipRegistrationNumber").asText());

        JsonNode renewal = body(postJson(token, "/api/memberships/" + id(membership) + "/renew", Map.of())
                .andExpect(status().isCreated()).andReturn());
        JsonNode renewalCharges = membershipCharges(token, id(renewal));
        assertEquals(1, renewalCharges.size(), "renewal has no setup fee");
        assertEquals("INSTALLMENT", renewalCharges.get(0).get("kind").asText());
        assertEquals(renewal.get("startDate").asText(), renewalCharges.get(0).get("dueDate").asText());
    }

    @Test
    void discountedAnnualQuarterlyMembershipUsesFinalAmountPerCycle() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Trimestral " + unique(""));
        JsonNode plan = createAnnualQuarterlyPlan(token, "300.00", "0.00", 0);
        Map<String, Object> payload = membershipPayload(studentId, id(plan), branchId);
        payload.put("discountAmount", "30.00");
        payload.put("discountReason", "Convênio");
        JsonNode membership = body(postJson(token, "/api/memberships", payload)
                .andExpect(status().isCreated()).andReturn());

        JsonNode generated = membershipCharges(token, id(membership));
        assertEquals(4, generated.size(), "no setup fee when it is zero");
        LocalDate start = LocalDate.parse(membership.get("startDate").asText());
        for (int index = 0; index < 4; index++) {
            JsonNode charge = generated.get(index);
            assertEquals("270.00", charge.get("amount").asText());
            assertEquals(index + 1, charge.get("installmentNumber").asInt());
            assertEquals(4, charge.get("installmentCount").asInt());
            assertEquals(start.plusMonths(3L * index).toString(), charge.get("dueDate").asText());
        }
    }

    @Test
    void cancellingMembershipCancelsFutureOpenChargesAndKeepsOverdueDebt() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Cancelamento " + unique(""));
        JsonNode plan = createAnnualQuarterlyPlan(token, "150.00", "40.00", 120);
        Map<String, Object> payload = membershipPayload(studentId, id(plan), branchId);
        LocalDate start = today().minusDays(100);
        payload.put("startDate", start.toString());
        JsonNode membership = body(postJson(token, "/api/memberships", payload)
                .andExpect(status().isCreated()).andReturn());

        JsonNode before = membershipCharges(token, id(membership));
        assertEquals(5, before.size());
        long overdue = 0;
        for (JsonNode charge : before) {
            if (charge.get("overdue").asBoolean()) {
                overdue++;
            }
        }
        assertEquals(3, overdue, "setup fee + installments 1 and 2 are past due");

        postJson(token, "/api/memberships/" + id(membership) + "/cancel", Map.of("reason", "Mudou de cidade"))
                .andExpect(status().isOk());

        JsonNode after = membershipCharges(token, id(membership));
        int open = 0;
        int cancelled = 0;
        for (JsonNode charge : after) {
            LocalDate due = LocalDate.parse(charge.get("dueDate").asText());
            if (due.isBefore(today())) {
                assertEquals("OPEN", charge.get("status").asText(), "overdue charges remain as debt");
                open++;
            } else {
                assertEquals("CANCELLED", charge.get("status").asText());
                assertTrue(charge.get("cancellationReason").asText().contains("Mudou de cidade"));
                cancelled++;
            }
        }
        assertEquals(3, open);
        assertEquals(2, cancelled);
    }

    @Test
    void paymentSettlesChargeRefundReopensIt() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Pagamento " + unique(""));
        JsonNode charge = manualCharge(token, studentId, branchId, "45.50", today().toString());
        String chargeUrl = "/api/charges/" + id(charge);

        postJson(token, chargeUrl + "/payments", payment("45.49", "PIX"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
        postJson(token, chargeUrl + "/payments", payment("45.50", "BITCOIN"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_METHOD"));
        Map<String, Object> future = payment("45.50", "PIX");
        future.put("paidAt", java.time.Instant.now().plusSeconds(3600).toString());
        postJson(token, chargeUrl + "/payments", future)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PAID_AT_IN_FUTURE"));

        Map<String, Object> pay = payment("45.50", "PIX");
        pay.put("reference", "E2E-123");
        JsonNode paid = body(postJson(token, chargeUrl + "/payments", pay)
                .andExpect(status().isCreated()).andReturn());
        assertEquals("PAID", paid.get("charge").get("status").asText());
        assertEquals(1, paid.get("payments").size());
        JsonNode receipt = paid.get("payments").get(0);
        assertEquals("CONFIRMED", receipt.get("status").asText());
        assertTrue(receipt.get("paymentNumber").asText().matches("R\\d{10}"));

        postJson(token, chargeUrl + "/payments", payment("45.50", "CASH"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CHARGE_NOT_OPEN"));
        postJson(token, chargeUrl + "/cancel", Map.of("reason", "x"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CHARGE_NOT_OPEN"));

        String refundUrl = "/api/payments/" + receipt.get("id").asText() + "/refund";
        postJson(token, refundUrl, Map.of("reason", "")).andExpect(status().isBadRequest());
        JsonNode refunded = body(postJson(token, refundUrl, Map.of("reason", "Pagamento em duplicidade"))
                .andExpect(status().isOk()).andReturn());
        assertEquals("OPEN", refunded.get("charge").get("status").asText());
        assertEquals("REFUNDED", refunded.get("payments").get(0).get("status").asText());
        postJson(token, refundUrl, Map.of("reason", "de novo"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_NOT_REFUNDABLE"));

        // Can be paid again after the refund; history keeps both receipts.
        JsonNode repaid = body(postJson(token, chargeUrl + "/payments", payment("45.50", "CASH"))
                .andExpect(status().isCreated()).andReturn());
        assertEquals(2, repaid.get("payments").size());

        Integer audits = com.gymsaas.shared.infrastructure.tenant.TenantContext.callWithBypass(() ->
                jdbcTemplate.queryForObject("""
                        select count(*) from audit_logs where resource_id = ?
                          and action in ('CHARGE_CREATED', 'PAYMENT_REGISTERED', 'PAYMENT_REFUNDED')
                        """, Integer.class, id(charge)));
        assertEquals(4, audits);
    }

    @Test
    void manualChargeValidationOverdueFilterAndCancellation() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Avulsa " + unique(""));
        UUID otherStudent = createStudent(token, "Outro Aluno " + unique(""));
        JsonNode plan = createPlan(token, "80.00");
        JsonNode membership = createMembership(token, otherStudent, id(plan), branchId);

        Map<String, Object> mismatch = new HashMap<>();
        mismatch.put("studentId", studentId.toString());
        mismatch.put("branchId", branchId.toString());
        mismatch.put("membershipId", id(membership).toString());
        mismatch.put("description", "Produto");
        mismatch.put("amount", "10.00");
        mismatch.put("dueDate", today().toString());
        postJson(token, "/api/charges", mismatch).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_STUDENT_MISMATCH"));
        mismatch.remove("membershipId");
        mismatch.put("amount", "0.00");
        postJson(token, "/api/charges", mismatch).andExpect(status().isBadRequest());
        mismatch.put("amount", "10.001");
        postJson(token, "/api/charges", mismatch).andExpect(status().isBadRequest());
        mismatch.put("amount", "10,00");
        postJson(token, "/api/charges", mismatch).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        getJson(token, "/api/charges?studentId=not-a-uuid").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        JsonNode late = manualCharge(token, studentId, branchId, "15.00", today().minusDays(3).toString());
        JsonNode current = manualCharge(token, studentId, branchId, "25.00", today().plusDays(3).toString());
        assertTrue(late.get("overdue").asBoolean());
        assertFalse(current.get("overdue").asBoolean());

        JsonNode overdueList = charges(token, "studentId=" + studentId + "&status=OVERDUE");
        assertEquals(1, overdueList.get("totalElements").asInt());
        assertEquals(id(late).toString(), overdueList.get("content").get(0).get("id").asText());
        assertEquals(2, charges(token, "studentId=" + studentId + "&status=OPEN").get("totalElements").asInt());
        getJson(token, "/api/charges?status=WHATEVER").andExpect(status().isBadRequest());
        getJson(token, "/api/charges?size=100&search=" + late.get("chargeNumber").asText())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(id(late).toString())));

        postJson(token, "/api/charges/" + id(late) + "/cancel", Map.of("reason", " ")).andExpect(status().isBadRequest());
        JsonNode cancelled = body(postJson(token, "/api/charges/" + id(late) + "/cancel",
                Map.of("reason", "Lançada por engano")).andExpect(status().isOk()).andReturn());
        assertEquals("CANCELLED", cancelled.get("charge").get("status").asText());
        assertFalse(cancelled.get("charge").get("overdue").asBoolean());
        postJson(token, "/api/charges/" + id(late) + "/payments", payment("15.00", "PIX"))
                .andExpect(status().isConflict());
        assertEquals(0, charges(token, "studentId=" + studentId + "&status=OVERDUE").get("totalElements").asInt());
    }

    @Test
    void summaryReportsReceivableOverdueAndReceivedByMethod() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        JsonNode before = body(getJson(token, "/api/billing/summary?branchId=" + branchId)
                .andExpect(status().isOk()).andReturn());

        UUID studentId = createStudent(token, "Aluno Resumo " + unique(""));
        JsonNode late = manualCharge(token, studentId, branchId, "11.00", today().minusDays(1).toString());
        JsonNode paidPix = manualCharge(token, studentId, branchId, "7.00", today().toString());
        JsonNode paidCash = manualCharge(token, studentId, branchId, "3.00", today().toString());
        JsonNode refundedCharge = manualCharge(token, studentId, branchId, "5.00", today().toString());
        postJson(token, "/api/charges/" + id(paidPix) + "/payments", payment("7.00", "PIX"))
                .andExpect(status().isCreated());
        postJson(token, "/api/charges/" + id(paidCash) + "/payments", payment("3.00", "CASH"))
                .andExpect(status().isCreated());
        JsonNode refundTarget = body(postJson(token, "/api/charges/" + id(refundedCharge) + "/payments",
                payment("5.00", "PIX")).andExpect(status().isCreated()).andReturn());
        postJson(token, "/api/payments/" + refundTarget.get("payments").get(0).get("id").asText() + "/refund",
                Map.of("reason", "Teste")).andExpect(status().isOk());

        JsonNode after = body(getJson(token, "/api/billing/summary?branchId=" + branchId)
                .andExpect(status().isOk()).andReturn());
        assertEquals(today().withDayOfMonth(1).toString(), after.get("from").asText());
        assertDelta(before, after, "overdue", 1, "11.00");
        assertDelta(before, after, "received", 2, "10.00");
        assertDelta(before, after, "refunded", 1, "5.00");
        // The refunded charge is open again and due today: receivable.
        assertDelta(before, after, "receivable", 1, "5.00");
        assertEquals(new java.math.BigDecimal("7.00"),
                method(after, "PIX").subtract(method(before, "PIX")));
        assertEquals(new java.math.BigDecimal("3.00"),
                method(after, "CASH").subtract(method(before, "CASH")));

        getJson(token, "/api/billing/summary?from=2026-02-01&to=2026-01-01").andExpect(status().isBadRequest());
        getJson(token, "/api/billing/summary?branchId=" + UUID.randomUUID()).andExpect(status().isNotFound());
        assertTrue(late.get("overdue").asBoolean());
    }

    private static void assertDelta(JsonNode before, JsonNode after, String field, int count, String total) {
        assertEquals(count, after.get(field).get("count").asLong() - before.get(field).get("count").asLong(), field);
        assertEquals(new java.math.BigDecimal(total), new java.math.BigDecimal(after.get(field).get("total").asText())
                .subtract(new java.math.BigDecimal(before.get(field).get("total").asText())), field);
    }

    private static java.math.BigDecimal method(JsonNode summary, String method) {
        for (JsonNode entry : summary.get("receivedByMethod")) {
            if (method.equals(entry.get("method").asText())) {
                return new java.math.BigDecimal(entry.get("total").asText());
            }
        }
        return java.math.BigDecimal.ZERO;
    }

    private static JsonNode find(JsonNode charges, String kind) {
        for (JsonNode charge : charges) {
            if (kind.equals(charge.get("kind").asText())) {
                return charge;
            }
        }
        throw new AssertionError("No charge of kind " + kind + " in " + charges);
    }
}
