package com.gymsaas.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymsaas.support.Phase5IntegrationSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 6 helpers on top of the Phase 5 fixtures. */
abstract class BillingIntegrationSupport extends Phase5IntegrationSupport {

    /** Annual plan billed quarterly; price valid since {@code daysAgo} so memberships can start in the past. */
    protected JsonNode createAnnualQuarterlyPlan(String token, String amount, String setupFee, int daysAgo)
            throws Exception {
        Map<String, Object> payload = planPayload(unique("PA"), amount);
        payload.put("durationType", "ANNUAL");
        payload.put("initialPrice", Map.of("amount", amount, "billingPeriod", "QUARTERLY", "setupFee", setupFee,
                "validFrom", today().minusDays(daysAgo).toString()));
        return body(postJson(token, "/api/plans", payload).andExpect(status().isCreated()).andReturn());
    }

    protected JsonNode charges(String token, String query) throws Exception {
        return body(getJson(token, "/api/charges?size=100&" + query).andExpect(status().isOk()).andReturn());
    }

    protected JsonNode membershipCharges(String token, UUID membershipId) throws Exception {
        return charges(token, "membershipId=" + membershipId + "&sort=dueDate&direction=asc").get("content");
    }

    protected JsonNode manualCharge(String token, UUID studentId, UUID branchId, String amount, String dueDate)
            throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", studentId.toString());
        payload.put("branchId", branchId.toString());
        payload.put("description", "Diária avulsa");
        payload.put("amount", amount);
        payload.put("dueDate", dueDate);
        return body(postJson(token, "/api/charges", payload).andExpect(status().isCreated()).andReturn())
                .get("charge");
    }

    protected Map<String, Object> payment(String amount, String method) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);
        payload.put("method", method);
        return payload;
    }
}
