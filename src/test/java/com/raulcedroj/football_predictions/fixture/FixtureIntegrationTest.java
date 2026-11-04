package com.raulcedroj.football_predictions.fixture;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FixtureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminCreatesFixtureAndSetsResult() throws Exception {
        String adminToken = tokenFor("admin-fixtures@example.com", true);
        String userToken = tokenFor("user-fixtures@example.com", false);
        Long roundId = firstRoundId();

        String created = mockMvc.perform(post("/api/admin/fixtures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roundId": %d, "homeTeam": "FC Barcelona", "awayTeam": "Real Madrid",
                                  "kickoffAt": "2026-08-16T19:00:00Z", "kickoffConfirmed": true }
                                """.formatted(roundId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn().getResponse().getContentAsString();

        Integer fixtureId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/rounds/{roundId}/fixtures", roundId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].homeTeam".formatted(fixtureId)).value("FC Barcelona"));

        mockMvc.perform(put("/api/admin/fixtures/{id}/result", fixtureId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "homeGoals": 2, "awayGoals": 1 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.homeGoals").value(2));
    }

    @Test
    void regularUserCannotCreateFixtures() throws Exception {
        String userToken = tokenFor("not-admin@example.com", false);

        mockMvc.perform(post("/api/admin/fixtures")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roundId": %d, "homeTeam": "Sevilla", "awayTeam": "Betis",
                                  "kickoffAt": "2026-08-17T19:00:00Z", "kickoffConfirmed": true }
                                """.formatted(firstRoundId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsFixtureWithSameTeams() throws Exception {
        String adminToken = tokenFor("admin-same-teams@example.com", true);

        mockMvc.perform(post("/api/admin/fixtures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roundId": %d, "homeTeam": "Girona", "awayTeam": "girona",
                                  "kickoffAt": "2026-08-17T19:00:00Z", "kickoffConfirmed": true }
                                """.formatted(firstRoundId())))
                .andExpect(status().isConflict());
    }

    private String tokenFor(String email, boolean admin) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "displayName": "Test", "password": "password123" }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        if (admin) {
            jdbcTemplate.update("UPDATE app_user SET role = 'ADMIN' WHERE email = ?", email);
        }

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "password123" }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }

    private Long firstRoundId() {
        return jdbcTemplate.queryForObject("""
                SELECT r.id FROM round r
                JOIN season s ON s.id = r.season_id
                WHERE s.name = '2026/27' AND r.sequence = 1
                """, Long.class);
    }
}