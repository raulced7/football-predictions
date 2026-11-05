package com.raulcedroj.football_predictions.standings;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StandingsIntegrationTest extends IntegrationTestSupport {

    @Test
    void calculatesStandingsWithTiesAndRevealsPredictionsOnlyAfterKickoff() throws Exception {
        String admin = tokenFor("std-admin@example.com", true);
        String alice = tokenFor("std-alice@example.com", false);
        String bob = tokenFor("std-bob@example.com", false);
        String carol = tokenFor("std-carol@example.com", false);
        String dave = tokenFor("std-dave@example.com", false);

        String group = createGroup(alice, "Clasificación").andReturn().getResponse().getContentAsString();
        Integer groupId = JsonPath.read(group, "$.id");
        String inviteCode = JsonPath.read(group, "$.inviteCode");
        join(bob, inviteCode);
        join(carol, inviteCode);
        join(dave, inviteCode);

        Long roundId = roundId(5);
        Instant tomorrow = Instant.now().plus(Duration.ofDays(1));
        Integer match1 = createFixture(admin, roundId, "Sevilla", "Betis", tomorrow);
        Integer match2 = createFixture(admin, roundId, "Atlético", "Espanyol", tomorrow);
        Integer match3 = createFixture(admin, roundId, "Alavés", "Rayo", tomorrow.plus(Duration.ofDays(1)));

        predict(alice, match1, 2, 1);
        predict(bob, match1, 1, 0);
        predict(carol, match1, 0, 0);
        predict(alice, match2, 1, 1);
        predict(bob, match2, 1, 1);
        predict(alice, match3, 3, 3);
        predict(bob, match3, 0, 1);

        setResult(admin, match1, 2, 1);   // alice 3, bob 1, carol 0
        setResult(admin, match2, 1, 1);   // alice 3, bob 3

        // General: alice 6 (2 exactos), bob 4 (1 exacto), carol y dave 0 → empatados en 3ª posición
        mockMvc.perform(get("/api/groups/{id}/standings", groupId).header("Authorization", "Bearer " + carol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].displayName").value("std-alice"))
                .andExpect(jsonPath("$[0].points").value(6))
                .andExpect(jsonPath("$[0].exactHits").value(2))
                .andExpect(jsonPath("$[1].displayName").value("std-bob"))
                .andExpect(jsonPath("$[1].position").value(2))
                .andExpect(jsonPath("$[2].position").value(3))
                .andExpect(jsonPath("$[3].position").value(3));

        mockMvc.perform(get("/api/groups/{id}/standings/rounds/{roundId}", groupId, roundId)
                        .header("Authorization", "Bearer " + carol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].points").value(6));

        // Los partidos terminados se revelan; el que no ha empezado, no
        String match3Item = "$[?(@.fixture.id == %d)]".formatted(match3);
        String match1Item = "$[?(@.fixture.id == %d)]".formatted(match1);
        mockMvc.perform(get("/api/groups/{id}/rounds/{roundId}/predictions", groupId, roundId)
                        .header("Authorization", "Bearer " + carol))
                .andExpect(status().isOk())
                .andExpect(jsonPath(match1Item + ".revealed").value(true))
                .andExpect(jsonPath(match1Item + ".predictions[*]", hasSize(3)))
                .andExpect(jsonPath(match3Item + ".revealed").value(false))
                .andExpect(jsonPath(match3Item + ".predictedCount").value(2))
                .andExpect(jsonPath(match3Item + ".predictions[*]", hasSize(0)));
    }

    @Test
    void nonMembersCannotSeeStandings() throws Exception {
        String owner = tokenFor("std-owner@example.com", false);
        String stranger = tokenFor("std-stranger@example.com", false);
        Integer groupId = JsonPath.read(createGroup(owner, "Secreto")
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/groups/{id}/standings", groupId).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
    }
}