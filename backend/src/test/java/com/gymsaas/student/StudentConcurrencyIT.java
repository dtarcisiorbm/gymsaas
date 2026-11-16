package com.gymsaas.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Collections;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentConcurrencyIT extends AbstractIntegrationTest {

    private static final String SAME_CPF = "52998224725";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void duplicateCpfUnderConcurrencyAllowsOnlyOneSuccess() throws Exception {
        String token = loginToken("admin@gymsaas.local", "Admin@123");
        int attempts = 12;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            String name = "Concorrente " + i;
            tasks.add(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                MvcResult result = mockMvc.perform(post("/api/students")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("name", name, "cpf", SAME_CPF))))
                        .andReturn();
                return result.getResponse().getStatus();
            });
        }

        List<Future<Integer>> futures = new ArrayList<>();
        for (Callable<Integer> task : tasks) {
            futures.add(executor.submit(task));
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long created = statuses.stream().filter(status -> status == 201).count();
        long conflicts = statuses.stream().filter(status -> status == 409).count();
        assertEquals(1, created, "exactly one student must be created for the same CPF");
        assertEquals(attempts - 1, conflicts, "all other attempts must conflict");
    }

    @Test
    void concurrentCreationsGenerateUniqueRegistrations() throws Exception {
        String token = loginToken("admin@gymsaas.local", "Admin@123");
        int attempts = 10;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            String name = "Sem CPF " + i;
            tasks.add(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                MvcResult result = mockMvc.perform(post("/api/students")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("name", name))))
                        .andExpect(status().isCreated())
                        .andReturn();
                return body(result).get("registrationNumber").asText();
            });
        }

        List<Future<String>> futures = new ArrayList<>();
        for (Callable<String> task : tasks) {
            futures.add(executor.submit(task));
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        List<String> registrations = new ArrayList<>();
        for (Future<String> future : futures) {
            registrations.add(future.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertEquals(attempts, registrations.size());
        assertEquals(attempts, registrations.stream().distinct().count(),
                "all registration numbers must be unique");
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).get("accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
