package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import com.raulcedroj.football_predictions.sync.provider.ExternalMatch;
import com.raulcedroj.football_predictions.sync.provider.FootballDataProvider;
import com.raulcedroj.football_predictions.sync.provider.ProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarSyncIntegrationTest extends IntegrationTestSupport {

    private static final Instant ROUND_30 = Instant.now().plus(Duration.ofDays(200)).truncatedTo(ChronoUnit.SECONDS);
    private static final Instant ROUND_31 = ROUND_30.plus(Duration.ofDays(7));

    @MockitoBean
    private FootballDataProvider provider;

    @Test
    void syncsCalendarAndAppliesChangesFromTheProvider() throws Exception {
        String admin = tokenFor("sync-admin@example.com", true);
        String user = tokenFor("sync-user@example.com", false);

        // 1ª sincronización: carga inicial
        providerReturns(
                match("fd-1", 30, ROUND_30, true, FixtureStatus.SCHEDULED, null, null),
                match("fd-2", 30, ROUND_30.plus(Duration.ofHours(2)), true, FixtureStatus.SCHEDULED, null, null),
                match("fd-3", 31, ROUND_31, false, FixtureStatus.SCHEDULED, null, null));
        sync(admin).andExpect(jsonPath("$.created").value(3));

        predict(user, fixtureId("fd-1"), 2, 0).andExpect(status().isOk());

        // 2ª: termina un partido, se aplaza otro y se confirma el horario del tercero
        providerReturns(
                match("fd-1", 30, ROUND_30, true, FixtureStatus.FINISHED, 2, 0),
                match("fd-2", 30, ROUND_30.plus(Duration.ofHours(2)), false, FixtureStatus.POSTPONED, null, null),
                match("fd-3", 31, ROUND_31, true, FixtureStatus.SCHEDULED, null, null));
        sync(admin).andExpect(jsonPath("$.updated").value(3));

        assertThat(column("status", "fd-1")).isEqualTo("FINISHED");
        assertThat(pointsOf("sync-user@example.com", "fd-1")).isEqualTo(3);
        assertThat(column("status", "fd-2")).isEqualTo("POSTPONED");
        assertThat(column("kickoff_confirmed", "fd-3")).isEqualTo("true");

        // 3ª: el aplazado recibe fecha en la semana de la jornada 31
        providerReturns(
                match("fd-1", 30, ROUND_30, true, FixtureStatus.FINISHED, 2, 0),
                match("fd-2", 30, ROUND_31.plus(Duration.ofDays(1)), true, FixtureStatus.SCHEDULED, null, null),
                match("fd-3", 31, ROUND_31, true, FixtureStatus.SCHEDULED, null, null));
        sync(admin)
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.unchanged").value(2));

        assertThat(column("status", "fd-2")).isEqualTo("SCHEDULED");
        assertThat(column("scoring_round_id", "fd-2")).isEqualTo(String.valueOf(roundId(31)));

        // 4ª: mismos datos, no cambia nada
        sync(admin).andExpect(jsonPath("$.unchanged").value(3));
    }

    @Test
    void returnsServiceUnavailableWhenProviderFails() throws Exception {
        String admin = tokenFor("sync-admin-down@example.com", true);
        when(provider.fetchMatches(any(), any()))
                .thenThrow(new ProviderUnavailableException("football-data.org no está disponible", null));

        sync(admin).andExpect(status().isServiceUnavailable());
    }

    private void providerReturns(ExternalMatch... matches) {
        when(provider.fetchMatches("PD", "2026")).thenReturn(List.of(matches));
    }

    private ResultActions sync(String adminToken) throws Exception {
        return mockMvc.perform(post("/api/admin/sync/seasons/{id}", currentSeasonId())
                .header("Authorization", "Bearer " + adminToken));
    }

    private static ExternalMatch match(String id, int matchday, Instant kickoff, boolean confirmed,
                                       FixtureStatus status, Integer home, Integer away) {
        return new ExternalMatch(id, matchday, kickoff, confirmed, status,
                "Local " + id, "Visitante " + id, home, away);
    }

    private Integer fixtureId(String externalId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM fixture WHERE external_id = ?", Integer.class, externalId);
    }

    private String column(String column, String externalId) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + "::text FROM fixture WHERE external_id = ?", String.class, externalId);
    }

    private Integer pointsOf(String email, String externalId) {
        return jdbcTemplate.queryForObject("""
                SELECT p.points FROM prediction p
                JOIN app_user u ON u.id = p.user_id
                JOIN fixture f ON f.id = p.fixture_id
                WHERE u.email = ? AND f.external_id = ?
                """, Integer.class, email, externalId);
    }
}