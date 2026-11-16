package com.gymsaas.membership;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymsaas.support.Phase5IntegrationSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Concurrent requests never produce overlapping memberships or double freezes. */
class MembershipConcurrencyIT extends Phase5IntegrationSupport {

    private static final int THREADS = 5;

    @Test
    void concurrentContractingOfSamePlanCreatesExactlyOneMembership() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Concorrência " + unique(""));
        UUID planId = id(createPlan(token, "60.00"));
        String payload = json(membershipPayload(studentId, planId, branchId));

        List<Integer> statuses = race(() -> mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/memberships")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(payload))
                .andReturn().getResponse().getStatus());

        assertEquals(1, statuses.stream().filter(code -> code == 201).count(), statuses.toString());
        assertTrue(statuses.stream().filter(code -> code != 201).allMatch(code -> code == 409), statuses.toString());
        JsonNode list = body(getJson(token, "/api/memberships?studentId=" + studentId).andReturn());
        assertEquals(1, list.get("totalElements").asInt());
    }

    @Test
    void concurrentFreezesApplyOnlyOnce() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Congelamento " + unique(""));
        UUID planId = id(createPlan(token, "60.00"));
        JsonNode membership = createMembership(token, studentId, planId, branchId);
        String url = "/api/memberships/" + membership.get("id").asText() + "/freeze";
        String payload = json(Map.of("endDate", today().plusDays(4).toString(), "reason", "Viagem"));

        List<Integer> statuses = race(() -> mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(payload))
                .andReturn().getResponse().getStatus());

        assertEquals(1, statuses.stream().filter(code -> code == 200).count(), statuses.toString());
        assertTrue(statuses.stream().filter(code -> code != 200).allMatch(code -> code == 409), statuses.toString());
        JsonNode detail = body(getJson(token, "/api/memberships/" + membership.get("id").asText()).andReturn());
        assertEquals(1, detail.get("freezes").size());
        assertEquals(java.time.LocalDate.parse(membership.get("endDate").asText()).plusDays(5).toString(),
                detail.get("endDate").asText(), "end date extended exactly once");
    }

    private List<Integer> race(Callable<Integer> request) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int index = 0; index < THREADS; index++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return request.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }
}
