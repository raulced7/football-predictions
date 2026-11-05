package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FixtureLifecycleIntegrationTest extends IntegrationTestSupport {

    @Test
    void postponedFixtureCountsInTheRoundWhereItIsPlayed() throws Exception {
        String admin = tokenFor("life-admin-1@example.com", true);
        Long round10 = roundId(10);
        Long round13 = roundId(13);
        Instant round10Start = Instant.now().plus(Duration.ofDays(10));
        Instant round13Start = Instant.now().plus(Duration.ofDays(31));

        createFixture(admin, round10, "Girona", "Leganés", round10Start);
        Integer moved = createFixture(admin, round10, "Valencia", "Betis", round10Start.plus(Duration.ofHours(2)));
        createFixture(admin, round13, "Sevilla", "Celta", round13Start);

        action(admin, moved, "postpone").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTPONED"));

        schedule(admin, moved, round13Start.plus(Duration.ofDays(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.roundId").value(round10))
                .andExpect(jsonPath("$.scoringRoundId").value(round13));

        mockMvc.perform(get("/api/rounds/{id}/fixtures", round13).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(moved)).isNotEmpty());
        mockMvc.perform(get("/api/rounds/{id}/fixtures", round10).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(moved)).isEmpty());
    }

    @Test
    void timeChangeWithinTheSameRoundKeepsTheRound() throws Exception {
        String admin = tokenFor("life-admin-2@example.com", true);
        Long round15 = roundId(15);
        Instant saturday = Instant.now().plus(Duration.ofDays(40));

        createFixture(admin, round15, "Osasuna", "Mallorca", saturday);
        Integer fixture = createFixture(admin, round15, "Getafe", "Alavés", saturday.plus(Duration.ofHours(3)));

        schedule(admin, fixture, saturday.plus(Duration.ofDays(2)))   // pasa al lunes
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoringRoundId").value(round15));
    }

    @Test
    void cancellingClearsPointsAndBlocksResults() throws Exception {
        String admin = tokenFor("life-admin-3@example.com", true);
        String user = tokenFor("life-user-3@example.com", false);
        Integer fixture = createFixture(admin, roundId(6), "Rayo", "Espanyol",
                Instant.now().plus(Duration.ofDays(1)));

        predict(user, fixture, 1, 0).andExpect(status().isOk());
        setResult(admin, fixture, 1, 0).andExpect(status().isOk());
        assertThat(points(fixture)).isEqualTo(3);

        action(admin, fixture, "cancel").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.homeGoals").doesNotExist());
        assertThat(points(fixture)).isNull();

        setResult(admin, fixture, 2, 0).andExpect(status().isConflict());
    }

    @Test
    void suspendedFixtureKeepsPredictionsClosed() throws Exception {
        String admin = tokenFor("life-admin-4@example.com", true);
        String user = tokenFor("life-user-4@example.com", false);
        Integer notStarted = createFixture(admin, roundId(7), "Athletic", "Villarreal",
                Instant.now().plus(Duration.ofDays(1)));
        Integer started = createFixture(admin, roundId(7), "Real Sociedad", "Elche",
                Instant.now().minus(Duration.ofMinutes(30)));

        action(admin, notStarted, "suspend").andExpect(status().isConflict());

        action(admin, started, "suspend").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        schedule(admin, started, Instant.now().plus(Duration.ofDays(3))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        predict(user, started, 1, 1).andExpect(status().isConflict());
    }

    private ResultActions action(String token, Integer fixtureId, String action) throws Exception {
        return mockMvc.perform(post("/api/admin/fixtures/{id}/{action}", fixtureId, action)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions schedule(String token, Integer fixtureId, Instant kickoffAt) throws Exception {
        return mockMvc.perform(put("/api/admin/fixtures/{id}/schedule", fixtureId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "kickoffAt": "%s", "kickoffConfirmed": true }
                        """.formatted(kickoffAt)));
    }

    private Integer points(Integer fixtureId) {
        return jdbcTemplate.queryForObject(
                "SELECT points FROM prediction WHERE fixture_id = ?", Integer.class, fixtureId);
    }
}