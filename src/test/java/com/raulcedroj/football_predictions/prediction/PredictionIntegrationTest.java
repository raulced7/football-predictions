package com.raulcedroj.football_predictions.prediction;

import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PredictionIntegrationTest extends IntegrationTestSupport {

    @Test
    void createsAndUpdatesPredictionBeforeKickoff() throws Exception {
        String admin = tokenFor("admin-pred-1@example.com", true);
        String user = tokenFor("user-pred-1@example.com", false);
        Long roundId = roundId(2);
        Integer fixtureId = createFixture(admin, roundId, "Valencia", "Villarreal",
                Instant.now().plus(Duration.ofDays(1)));

        predict(user, fixtureId, 1, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeGoals").value(1));

        predict(user, fixtureId, 3, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeGoals").value(3))
                .andExpect(jsonPath("$.awayGoals").value(0));

        String item = "$[?(@.fixture.id == %d)]".formatted(fixtureId);
        mockMvc.perform(get("/api/rounds/{roundId}/predictions/me", roundId)
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath(item + ".open").value(true))
                .andExpect(jsonPath(item + ".prediction.homeGoals").value(3));
    }

    @Test
    void rejectsPredictionAfterKickoff() throws Exception {
        String admin = tokenFor("admin-pred-2@example.com", true);
        String user = tokenFor("user-pred-2@example.com", false);
        Integer fixtureId = createFixture(admin, roundId(2), "Getafe", "Osasuna",
                Instant.now().minus(Duration.ofMinutes(5)));

        predict(user, fixtureId, 0, 0).andExpect(status().isConflict());
    }

    @Test
    void predictionsArePrivateToEachUser() throws Exception {
        String admin = tokenFor("admin-pred-3@example.com", true);
        String userA = tokenFor("user-a-pred@example.com", false);
        String userB = tokenFor("user-b-pred@example.com", false);
        Long roundId = roundId(3);
        Integer fixtureId = createFixture(admin, roundId, "Mallorca", "Celta",
                Instant.now().plus(Duration.ofDays(2)));

        predict(userA, fixtureId, 2, 2).andExpect(status().isOk());

        mockMvc.perform(get("/api/rounds/{roundId}/predictions/me", roundId)
                        .header("Authorization", "Bearer " + userB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.fixture.id == %d)].prediction".formatted(fixtureId))
                        .value(org.hamcrest.Matchers.contains((Object) null)));
    }

    @Test
    void returnsNotFoundForUnknownFixture() throws Exception {
        String user = tokenFor("user-pred-4@example.com", false);
        predict(user, 999999, 1, 0).andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions predict(
            String token, Integer fixtureId, int home, int away) throws Exception {
        return mockMvc.perform(put("/api/fixtures/{id}/prediction", fixtureId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "homeGoals": %d, "awayGoals": %d }
                        """.formatted(home, away)));
    }
}