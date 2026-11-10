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
import org.springframework.test.web.servlet.ResultActions;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.sync.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    // ---------- Usuarios ----------

    protected String tokenFor(String email, boolean admin) throws Exception {
        String displayName = email.substring(0, email.indexOf('@'));   // ← EL CAMBIO: "std-alice@example.com" → "std-alice"

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "displayName": "%s", "password": "password123" }
                                """.formatted(email, displayName)))
                .andExpect(status().isCreated());

        // Los tests no necesitan el correo: el email se da por verificado directamente
        jdbcTemplate.update("UPDATE app_user SET email_verified_at = now() WHERE email = ?", email);

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

    // ---------- Temporadas y jornadas ----------

    protected Long currentSeasonId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM season WHERE name = '2026/27'", Long.class);
    }

    protected Long roundId(int sequence) {
        return jdbcTemplate.queryForObject("""
                SELECT r.id FROM round r
                JOIN season s ON s.id = r.season_id
                WHERE s.name = '2026/27' AND r.sequence = ?
                """, Long.class, sequence);
    }

    // ---------- Partidos ----------

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

    protected ResultActions setResult(String adminToken, Integer fixtureId, int home, int away) throws Exception {
        return mockMvc.perform(put("/api/admin/fixtures/{id}/result", fixtureId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "homeGoals": %d, "awayGoals": %d }
                        """.formatted(home, away)));
    }

    // ---------- Pronósticos ----------

    protected ResultActions predict(String token, Integer fixtureId, int home, int away) throws Exception {
        return mockMvc.perform(put("/api/fixtures/{id}/prediction", fixtureId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "homeGoals": %d, "awayGoals": %d }
                        """.formatted(home, away)));
    }

    // ---------- Grupos ----------

    protected ResultActions createGroup(String token, String name) throws Exception {
        return mockMvc.perform(post("/api/groups")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "%s", "seasonId": %d }
                        """.formatted(name, currentSeasonId())));
    }

    protected ResultActions join(String token, String inviteCode) throws Exception {
        return mockMvc.perform(post("/api/groups/join")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "inviteCode": "%s" }
                        """.formatted(inviteCode)));
    }
}