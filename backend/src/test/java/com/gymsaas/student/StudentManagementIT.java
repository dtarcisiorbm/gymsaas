package com.gymsaas.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentManagementIT extends AbstractIntegrationTest {

    private static final String VALID_CPF = "11144477735";
    private static final String OTHER_CPF = "93541134780";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void fullStudentLifecycle() throws Exception {
        String token = loginToken("admin@gymsaas.local", "Admin@123");
        String branchId = demoBranchId(token);

        // Create
        Map<String, Object> create = new HashMap<>();
        create.put("name", "Maria Silva");
        create.put("socialName", "Mari");
        create.put("birthDate", "1990-05-10");
        create.put("email", "maria.silva@example.com");
        create.put("phone", "(11) 98888-7777");
        create.put("gender", "FEMALE");
        create.put("notes", "Observação inicial");
        create.put("cpf", VALID_CPF);
        create.put("primaryBranchId", branchId);

        MvcResult created = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationNumber").isString())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.cpfMasked").value("***.***.***-35"))
                .andReturn();
        String studentId = body(created).get("id").asText();

        // Duplicate CPF in same tenant
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Outra", "cpf", "111.444.777-35"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CPF_ALREADY_EXISTS"));

        // Invalid CPF
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Inválida", "cpf", "111.111.111-11"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CPF"));

        // List and search
        mockMvc.perform(get("/api/students")
                        .param("search", "Maria")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(studentId)));

        mockMvc.perform(get("/api/students")
                        .param("document", "111444")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(studentId)));

        mockMvc.perform(get("/api/students")
                        .param("status", "PENDING")
                        .param("branchId", branchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(studentId)));

        // Detail masked
        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpfMasked").value("***.***.***-35"))
                .andExpect(jsonPath("$.cpf").doesNotExist());

        // Reveal documents (admin has read_sensitive)
        mockMvc.perform(get("/api/students/{id}/documents", studentId)
                        .param("reveal", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(VALID_CPF));

        // Update
        Map<String, Object> update = new HashMap<>();
        update.put("name", "Maria Souza");
        update.put("socialName", "Mari S");
        update.put("birthDate", "1990-05-10");
        update.put("email", "maria.souza@example.com");
        update.put("phone", "(11) 97777-6666");
        update.put("gender", "FEMALE");
        update.put("notes", "Atualizada");
        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Souza"))
                .andExpect(jsonPath("$.socialName").value("Mari S"));

        // Status change
        mockMvc.perform(patch("/api/students/{id}/status", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE", "reason", "Aprovada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/students/{id}/status", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PENDING"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        // Address
        mockMvc.perform(put("/api/students/{id}/address", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("zipCode", "01001-000", "street", "Praça da Sé",
                                "number", "100", "neighborhood", "Sé", "city", "São Paulo", "state", "SP"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Praça da Sé"));

        mockMvc.perform(get("/api/students/{id}/address", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("São Paulo"));

        // Contact
        MvcResult contactRes = mockMvc.perform(post("/api/students/{id}/contacts", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "João", "relationship", "Pai",
                                "phone", "(11) 90000-0001", "primary", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primary").value(true))
                .andReturn();
        String contactId = body(contactRes).get("id").asText();

        mockMvc.perform(put("/api/students/{id}/contacts/{cid}", studentId, contactId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "João Silva", "relationship", "Pai",
                                "phone", "(11) 90000-0002", "primary", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Silva"));

        // Additional document
        MvcResult docRes = mockMvc.perform(post("/api/students/{id}/documents", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("type", "RG", "number", "12.345.678-9", "issuer", "SSP", "state", "SP"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RG"))
                .andReturn();
        String docId = body(docRes).get("id").asText();

        mockMvc.perform(delete("/api/students/{id}/documents/{did}", studentId, docId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/students/{id}/contacts/{cid}", studentId, contactId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Branch assignment, transfer and history
        String newBranchId = createBranch(token, "BRSTUD" + suffix());
        mockMvc.perform(post("/api/students/{id}/branches", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("branchId", newBranchId, "primary", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.branchId").value(newBranchId));

        mockMvc.perform(post("/api/students/{id}/transfer", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("branchId", newBranchId, "reason", "Mudança"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchId").value(newBranchId))
                .andExpect(jsonPath("$.primary").value(true));

        // Notes
        mockMvc.perform(post("/api/students/{id}/notes", studentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Aluno pediu contato."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Aluno pediu contato."));

        mockMvc.perform(get("/api/students/{id}/notes", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));

        // History
        mockMvc.perform(get("/api/students/{id}/history", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)));

        // Pagination clamp
        mockMvc.perform(get("/api/students")
                        .param("size", "1000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        // Soft delete
        mockMvc.perform(delete("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String createBranch(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Unidade " + code, "code", code, "city", "São Paulo", "state", "SP"))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }

    private String demoBranchId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/branches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = body(result).get("content");
        if (content != null && content.size() > 0) {
            return content.get(0).get("id").asText();
        }
        return createBranch(token, "BRDEMO" + suffix());
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

    private String suffix() {
        return String.valueOf(Math.abs(System.nanoTime()) % 100000);
    }
}
