package com.raulcedroj.football_predictions.support;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String tokenFor(String email, boolean admin) throws Exception {
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

    protected Long roundId(int sequence) {
        return jdbcTemplate.queryForObject("""
                SELECT r.id FROM round r
                JOIN season s ON s.id = r.season_id
                WHERE s.name = '2026/27' AND r.sequence = ?
                """, Long.class, sequence);
    }

    protected Integer createFixture(String adminToken, Long roundId, String home, String away,
                                    Instant kickoffAt) throws Exception {
        String response = mockMvc.perform(post("/api/admin/fixtures")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roundId": %d, "homeTeam": "%s", "awayTeam": "%s",
                                  "kickoffAt": "%s", "kickoffConfirmed": true }
                                """.formatted(roundId, home, away, kickoffAt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.id");
    }
}