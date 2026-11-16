package com.gymsaas.plan;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanManagementIT extends Phase5IntegrationSupport {

    @Test
    void planPriceHistoryAndFeatures() throws Exception {
        String token = adminToken();
        JsonNode plan = createPlan(token, "100.00");
        String planId = plan.get("id").asText();
        assertEquals("100.00", plan.at("/currentPrice/amount").asText());
        assertEquals("20.00", plan.at("/currentPrice/setupFee").asText());

        postJson(token, "/api/plans", planPayload(plan.get("code").asText(), "50.00"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PLAN_CODE_EXISTS"));

        postJson(token, "/api/plans/" + planId + "/prices", Map.of("amount", "110.00", "billingPeriod", "MONTHLY",
                "validFrom", today().minusDays(1).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PRICE_RETROACTIVE"));

        LocalDate future = today().plusDays(30);
        JsonNode scheduled = body(postJson(token, "/api/plans/" + planId + "/prices", Map.of("amount", "120.00",
                "billingPeriod", "MONTHLY", "validFrom", future.toString()))
                .andExpect(status().isCreated()).andReturn());
        assertTrue(scheduled.get("scheduled").asBoolean());

        getJson(token, "/api/plans/" + planId).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice.amount").value("100.00"));

        JsonNode history = body(getJson(token, "/api/plans/" + planId + "/prices").andExpect(status().isOk())
                .andReturn());
        assertEquals(2, history.size());
        assertEquals("120.00", history.get(0).get("amount").asText());
        assertEquals(future.minusDays(1).toString(), history.get(1).get("validUntil").asText());

        postJson(token, "/api/plans/" + planId + "/prices/" + scheduled.get("id").asText() + "/cancel",
                Map.of("reason", "Revisão comercial"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        history = body(getJson(token, "/api/plans/" + planId + "/prices").andReturn());
        assertEquals("CANCELLED", history.get(0).get("status").asText());
        assertFalse(history.get(1).hasNonNull("validUntil"), "previous price must be reopened");

        UUID branchId = demoBranchId(token);
        putJson(token, "/api/plans/" + planId + "/features", Map.of("features", List.of(
                Map.of("type", "BRANCH_ACCESS", "branchId", branchId.toString()),
                Map.of("type", "TIME_WINDOW", "startTime", "06:00", "endTime", "12:00", "daysOfWeek", 31),
                Map.of("type", "WEEKLY_CHECKIN_LIMIT", "intValue", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features.length()").value(3))
                .andExpect(jsonPath("$.features[0].branchName").value("Unidade Centro"));

        putJson(token, "/api/plans/" + planId + "/features", Map.of("features", List.of(
                Map.of("type", "TIME_WINDOW", "startTime", "12:00", "endTime", "06:00", "daysOfWeek", 1))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FEATURE"));

        putJson(token, "/api/plans/" + planId + "/features", Map.of("features", List.of(
                Map.of("type", "BRANCH_ACCESS", "branchId", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));

        Map<String, Object> update = new HashMap<>(Map.of("name", "Plano renomeado",
                "durationType", "MONTHLY", "durationValue", 1, "version", 999));
        putJson(token, "/api/plans/" + planId, update)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
        update.remove("version");
        putJson(token, "/api/plans/" + planId, update)
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Plano renomeado"));

        patchJson(token, "/api/plans/" + planId + "/status", Map.of("status", "INACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        JsonNode inactive = body(getJson(token, "/api/plans?status=INACTIVE&size=100").andReturn());
        assertTrue(inactive.get("content").findValuesAsText("id").contains(planId));
    }

    @Test
    void invalidDurationAndIncompatibleBillingAreRejected() throws Exception {
        String token = adminToken();
        Map<String, Object> invalid = planPayload(unique("P"), "10.00");
        invalid.put("durationValue", 3);
        postJson(token, "/api/plans", invalid)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DURATION"));

        Map<String, Object> days = planPayload(unique("P"), "10.00");
        days.put("durationType", "CUSTOM_DAYS");
        days.put("durationValue", 30);
        postJson(token, "/api/plans", days)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BILLING_PERIOD_INCOMPATIBLE"));
    }

    @Test
    void receptionistCanReadButNotManagePlans() throws Exception {
        String token = receptionToken();
        getJson(token, "/api/plans").andExpect(status().isOk());
        postJson(token, "/api/plans", planPayload(unique("P"), "10.00")).andExpect(status().isForbidden());
    }

    @Test
    void priceRowsAreImmutableInTheDatabase() throws Exception {
        JsonNode plan = createPlan(adminToken(), "100.00");
        UUID planId = id(plan);
        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() ->
                jdbcTemplate.update("update plan_prices set amount = 1 where plan_id = ?", planId)));
        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() ->
                jdbcTemplate.update("delete from plan_prices where plan_id = ?", planId)));
    }
}
