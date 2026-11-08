package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import com.raulcedroj.football_predictions.sync.provider.ExternalMatch;
import com.raulcedroj.football_predictions.sync.provider.ExternalTeam;
import com.raulcedroj.football_predictions.sync.provider.FootballDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamSyncIntegrationTest extends IntegrationTestSupport {

    private static final Instant KICKOFF = Instant.parse("2031-08-17T18:00:00Z");

    @MockitoBean
    private FootballDataProvider provider;

    @Test
    void createsTeamsWithCrestsAndLinksTeamsCreatedByHand() throws Exception {
        String admin = tokenFor("teams-admin@example.com", true);
        String user = tokenFor("teams-user@example.com", false);
        Long seasonId = createTeamsSeason();
        Long round1 = roundOf(seasonId, 1);

        // Un partido creado a mano antes de sincronizar: el equipo "Prueba" aún no tiene datos del proveedor
        Integer manualFixture = createFixture(admin, round1, "Prueba", "Rival Manual", KICKOFF.minusSeconds(3600));
        Long manualTeamId = teamIdByName("Prueba");

        // Mismo nombre con otras mayúsculas: se reutiliza el equipo
        createFixture(admin, round1, "prueba", "Otro Rival", KICKOFF.minusSeconds(7200));
        assertThat(countTeams("lower(name) = 'prueba'")).isEqualTo(1);

        ExternalTeam prueba = new ExternalTeam("9001", "Club Deportivo Prueba", "Prueba", "PRU",
                "https://crests.example.com/9001.svg");
        ExternalTeam ensayo = new ExternalTeam("9002", "Unión Ensayo", "Ensayo", "ENS",
                "https://crests.example.com/9002.png");
        providerReturns(match("tms-1", prueba, ensayo));
        sync(admin, seasonId);

        // "Prueba" se vincula al proveedor en lugar de duplicarse, y "Ensayo" se crea nuevo
        assertThat(teamIdByExternalId("9001")).isEqualTo(manualTeamId);
        assertThat(countTeams("external_id IN ('9001', '9002')")).isEqualTo(2);

        mockMvc.perform(get("/api/rounds/{roundId}/fixtures", round1)
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].homeTeam.name".formatted(manualFixture))
                        .value("Club Deportivo Prueba"))
                .andExpect(jsonPath("$[?(@.homeTeam.tla == 'PRU' && @.awayTeam.tla == 'ENS')].homeTeam.shortName")
                        .value("Prueba"))
                .andExpect(jsonPath("$[?(@.awayTeam.tla == 'ENS')].awayTeam.crestUrl")
                        .value("https://crests.example.com/9002.png"));

        // Si el proveedor cambia el escudo, se actualiza sin crear otro equipo
        providerReturns(match("tms-1", prueba, new ExternalTeam("9002", "Unión Ensayo", "Ensayo", "ENS",
                "https://crests.example.com/9002-new.svg")));
        sync(admin, seasonId);

        assertThat(countTeams("external_id = '9002'")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT crest_url FROM team WHERE external_id = '9002'", String.class))
                .isEqualTo("https://crests.example.com/9002-new.svg");
    }

    private Long createTeamsSeason() {
        jdbcTemplate.update("INSERT INTO competition (code, name, external_id) VALUES ('TEAMS', 'Teams League', 'TMS')");
        Long seasonId = jdbcTemplate.queryForObject("""
                INSERT INTO season (competition_id, name, start_date, end_date, status, external_id)
                SELECT id, '2031/32', DATE '2031-08-01', DATE '2032-05-31', 'UPCOMING', '2031'
                FROM competition WHERE code = 'TEAMS'
                RETURNING id
                """, Long.class);
        jdbcTemplate.update("""
                INSERT INTO round (season_id, sequence, name)
                SELECT ?, n, 'Jornada ' || n FROM generate_series(1, 2) AS n
                """, seasonId);
        return seasonId;
    }

    private Long roundOf(Long seasonId, int sequence) {
        return jdbcTemplate.queryForObject("SELECT id FROM round WHERE season_id = ? AND sequence = ?",
                Long.class, seasonId, sequence);
    }

    private Long teamIdByName(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM team WHERE name = ?", Long.class, name);
    }

    private Long teamIdByExternalId(String externalId) {
        return jdbcTemplate.queryForObject("SELECT id FROM team WHERE external_id = ?", Long.class, externalId);
    }

    private Integer countTeams(String condition) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM team WHERE " + condition, Integer.class);
    }

    private void providerReturns(ExternalMatch... matches) {
        when(provider.fetchMatches("TMS", "2031")).thenReturn(List.of(matches));
    }

    private void sync(String adminToken, Long seasonId) throws Exception {
        mockMvc.perform(post("/api/admin/sync/seasons/{id}", seasonId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private static ExternalMatch match(String id, ExternalTeam home, ExternalTeam away) {
        return new ExternalMatch(id, 1, KICKOFF, true, FixtureStatus.SCHEDULED, home, away, null, null);
    }
}