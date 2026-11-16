package com.gymsaas.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymsaas.shared.domain.Sha256;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.support.Phase5IntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContractIT extends Phase5IntegrationSupport {

    @Test
    void generateDownloadAndAcceptContract() throws Exception {
        String token = receptionToken();
        JsonNode membership = newMembership();
        String membershipId = membership.get("id").asText();

        JsonNode contract = body(postJson(token, "/api/memberships/" + membershipId + "/contracts", Map.of())
                .andExpect(status().isCreated()).andReturn());
        assertEquals(1, contract.get("contractVersion").asInt());
        assertEquals("GENERATED", contract.get("status").asText());
        String content = contract.get("content").asText();
        assertEquals(Sha256.hex(content), contract.get("contentSha256").asText());
        assertTrue(content.contains(membership.get("registrationNumber").asText()));
        assertTrue(content.contains("Cláusulas de teste definidas pela academia."));
        assertTrue(contract.get("legalNotice").asText().contains("Não constitui assinatura digital qualificada"));
        String contractId = contract.get("id").asText();

        MockHttpServletResponse pdf = getJson(token, "/api/contracts/" + contractId + "/document")
                .andExpect(status().isOk()).andReturn().getResponse();
        assertEquals("application/pdf", pdf.getContentType());
        byte[] bytes = pdf.getContentAsByteArray();
        assertTrue(new String(bytes, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF"));
        assertEquals(contract.get("documentSha256").asText(), Sha256.hex(bytes));
        String storageKey = TenantContext.callWithBypass(() -> jdbcTemplate.queryForObject(
                "select storage_key from membership_contracts where id = ?::uuid", String.class, contractId));
        assertTrue(Files.exists(CONTRACT_STORAGE.resolve(storageKey)), "PDF stored outside the database");

        postJson(token, "/api/contracts/" + contractId + "/acceptance", Map.of("acceptorName", "Aluno",
                "contentSha256", "0".repeat(64), "confirmed", true))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTRACT_CONTENT_MISMATCH"));
        postJson(token, "/api/contracts/" + contractId + "/acceptance", Map.of("acceptorName", "Aluno",
                "contentSha256", contract.get("contentSha256").asText(), "confirmed", false))
                .andExpect(status().isBadRequest());

        JsonNode accepted = body(postJson(token, "/api/contracts/" + contractId + "/acceptance",
                Map.of("acceptorName", "Aluno Teste", "contentSha256", contract.get("contentSha256").asText(),
                        "confirmed", true))
                .andExpect(status().isCreated()).andReturn());
        assertEquals("ACCEPTED", accepted.get("status").asText());
        assertEquals("IN_PERSON", accepted.at("/acceptance/channel").asText());
        assertEquals("Aluno Teste", accepted.at("/acceptance/acceptorName").asText());
        assertEquals(contract.get("contentSha256").asText(), accepted.at("/acceptance/contentSha256").asText());
        assertTrue(accepted.at("/acceptance/declaration").asText().contains("assinatura digital qualificada"));

        postJson(token, "/api/memberships/" + membershipId + "/contracts", Map.of())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTRACT_ALREADY_ACCEPTED"));

        JsonNode history = body(getJson(token, "/api/memberships/" + membershipId + "/history").andReturn());
        assertTrue(history.findValuesAsText("type").containsAll(List.of("CONTRACT_GENERATED", "CONTRACT_ACCEPTED")));

        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() -> jdbcTemplate.update(
                "update membership_contracts set content = 'adulterado' where id = ?::uuid", contractId)));
        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() -> jdbcTemplate.update(
                "delete from contract_acceptances where contract_id = ?::uuid", contractId)));
    }

    @Test
    void regenerationSupersedesUnacceptedVersion() throws Exception {
        String token = adminToken();
        String membershipId = newMembership().get("id").asText();
        JsonNode v1 = body(postJson(token, "/api/memberships/" + membershipId + "/contracts", Map.of())
                .andExpect(status().isCreated()).andReturn());
        JsonNode v2 = body(postJson(token, "/api/memberships/" + membershipId + "/contracts", Map.of())
                .andExpect(status().isCreated()).andReturn());
        assertEquals(2, v2.get("contractVersion").asInt());

        JsonNode versions = body(getJson(token, "/api/memberships/" + membershipId + "/contracts")
                .andExpect(status().isOk()).andReturn());
        assertEquals(2, versions.size());
        assertEquals("GENERATED", versions.get(0).get("status").asText());
        assertEquals("SUPERSEDED", versions.get(1).get("status").asText());

        postJson(token, "/api/contracts/" + v1.get("id").asText() + "/acceptance", Map.of("acceptorName", "Aluno",
                "contentSha256", v1.get("contentSha256").asText(), "confirmed", true))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTRACT_SUPERSEDED"));
    }

    @Test
    void concurrentAcceptancesRegisterExactlyOnce() throws Exception {
        String token = adminToken();
        String membershipId = newMembership().get("id").asText();
        JsonNode contract = body(postJson(token, "/api/memberships/" + membershipId + "/contracts", Map.of())
                .andExpect(status().isCreated()).andReturn());
        String payload = json(Map.of("acceptorName", "Aluno", "contentSha256",
                contract.get("contentSha256").asText(), "confirmed", true));
        String url = "/api/contracts/" + contract.get("id").asText() + "/acceptance";

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < 4; index++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return mockMvc.perform(post(url).header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON).content(payload))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            assertEquals(1, statuses.stream().filter(code -> code == 201).count(), statuses.toString());
            assertTrue(statuses.stream().filter(code -> code != 201).allMatch(code -> code == 409),
                    statuses.toString());
        } finally {
            pool.shutdownNow();
        }
        Integer acceptances = TenantContext.callWithBypass(() -> jdbcTemplate.queryForObject(
                "select count(*)::int from contract_acceptances where contract_id = ?::uuid", Integer.class,
                contract.get("id").asText()));
        assertEquals(1, acceptances);
    }

    @Test
    void permissionsAreEnforced() throws Exception {
        // Receptionist has contracts:* but cannot cancel memberships (no memberships:cancel).
        String membershipId = newMembership().get("id").asText();
        postJson(receptionToken(), "/api/memberships/" + membershipId + "/cancel", Map.of("reason", "x"))
                .andExpect(status().isForbidden());
        getJson(receptionToken(), "/api/contracts/" + UUID.randomUUID()).andExpect(status().isNotFound());
    }

    private JsonNode newMembership() throws Exception {
        String admin = adminToken();
        UUID branchId = demoBranchId(admin);
        UUID planId = id(createPlan(admin, "99.90"));
        UUID studentId = createStudent(admin, "Aluno Contrato " + unique(""));
        return createMembership(admin, studentId, planId, branchId);
    }
}
