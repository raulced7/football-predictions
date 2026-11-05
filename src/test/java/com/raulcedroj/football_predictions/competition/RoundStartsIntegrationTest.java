package com.raulcedroj.football_predictions.competition;

import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoundStartsIntegrationTest extends IntegrationTestSupport {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Test
    void broughtForwardFixtureDoesNotCaptureOtherRescheduledFixtures() throws Exception {
        String admin = tokenFor("starts-admin-1@example.com", true);
        Instant round11Start = NOW.plus(Duration.ofDays(20));
        Instant round19Start = NOW.plus(Duration.ofDays(60));

        createFixture(admin, roundId(11), "Getafe", "Celta", round11Start);
        Integer postponed = createFixture(admin, roundId(11), "Betis", "Osasuna", round11Start.plus(Duration.ofHours(3)));
        createFixture(admin, roundId(19), "Barcelona", "Girona", round19Start);
        createFixture(admin, roundId(19), "Sevilla", "Mallorca", round19Start.plus(Duration.ofDays(1)));
        // Partido de la J19 adelantado a la semana de la J11
        createFixture(admin, roundId(19), "Real Madrid", "Valencia", round11Start.plus(Duration.ofDays(1)));

        assertThat(startOf(19)).isEqualTo(round19Start);

        mockMvc.perform(post("/api/admin/fixtures/{id}/postpone", postponed)
                .header("Authorization", "Bearer " + admin)).andExpect(status().isOk());

        reschedule(admin, postponed, round11Start.plus(Duration.ofDays(2)))
                .andExpect(jsonPath("$.scoringRoundId").value(roundId(11)));
    }

    @Test
    void startIsFrozenOnceTheRoundHasStarted() throws Exception {
        String admin = tokenFor("starts-admin-2@example.com", true);
        Instant twoHoursAgo = NOW.minus(Duration.ofHours(2));

        createFixture(admin, roundId(9), "Alavés", "Rayo", twoHoursAgo);
        createFixture(admin, roundId(9), "Elche", "Levante", NOW.minus(Duration.ofHours(1)));
        assertThat(startOf(9)).isEqualTo(twoHoursAgo);

        createFixture(admin, roundId(9), "Oviedo", "Espanyol", NOW.minus(Duration.ofHours(3)));
        assertThat(startOf(9)).isEqualTo(twoHoursAgo);
    }

    @Test
    void startFollowsConfirmedTimesBeforeTheRoundStarts() throws Exception {
        String admin = tokenFor("starts-admin-3@example.com", true);
        Instant provisionalSunday = NOW.plus(Duration.ofDays(120));

        Integer fixture = createFixture(admin, roundId(22), "Athletic", "Villarreal", provisionalSunday);
        createFixture(admin, roundId(22), "Real Sociedad", "Atlético", provisionalSunday);
        assertThat(startOf(22)).isEqualTo(provisionalSunday);

        // LaLiga confirma el partido para el viernes
        Instant confirmedFriday = provisionalSunday.minus(Duration.ofDays(2));
        reschedule(admin, fixture, confirmedFriday)
                .andExpect(jsonPath("$.scoringRoundId").value(roundId(22)));

        assertThat(startOf(22)).isEqualTo(confirmedFriday);
    }

    private org.springframework.test.web.servlet.ResultActions reschedule(
            String token, Integer fixtureId, Instant kickoffAt) throws Exception {
        return mockMvc.perform(put("/api/admin/fixtures/{id}/schedule", fixtureId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "kickoffAt": "%s", "kickoffConfirmed": true }
                                """.formatted(kickoffAt)))
                .andExpect(status().isOk());
    }

    private Instant startOf(int sequence) {
        Timestamp startsAt = jdbcTemplate.queryForObject(
                "SELECT starts_at FROM round WHERE id = ?", Timestamp.class, roundId(sequence));
        return startsAt == null ? null : startsAt.toInstant();
    }
}