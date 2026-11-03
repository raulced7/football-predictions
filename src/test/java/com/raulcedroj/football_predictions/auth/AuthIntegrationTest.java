package com.raulcedroj.football_predictions.auth;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersLogsInAndAccessesProtectedEndpoint() throws Exception {
        register("raul@example.com", "Raúl", "password123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("raul@example.com"));

        String token = login("RAUL@example.com", "password123");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Raúl"));
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        register("ana@example.com", "Ana", "password123").andExpect(status().isCreated());
        register("ANA@example.com", "Ana 2", "password123").andExpect(status().isConflict());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        register("pepe@example.com", "Pepe", "password123").andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "pepe@example.com", "password": "wrong-password" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String email, String name, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "displayName": "%s", "password": "%s" }
                        """.formatted(email, name, password)));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}