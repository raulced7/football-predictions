package com.raulcedroj.football_predictions.sync.footballdata;

import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import com.raulcedroj.football_predictions.sync.provider.ExternalMatch;
import com.raulcedroj.football_predictions.sync.provider.ProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FootballDataOrgProviderTest {

    private static final String URL = "https://api.football-data.org/v4/competitions/PD/matches?season=2026";

    private static final String JSON = """
            {
              "matches": [
                { "id": 1001, "utcDate": "2026-08-15T17:00:00Z", "status": "FINISHED", "matchday": 1,
                  "homeTeam": { "name": "FC Barcelona", "shortName": "Barça" },
                  "awayTeam": { "name": "Real Madrid CF", "shortName": "Real Madrid" },
                  "score": { "fullTime": { "home": 2, "away": 1 } } },
                { "id": 1002, "utcDate": "2026-08-16T19:00:00Z", "status": "TIMED", "matchday": 1,
                  "homeTeam": { "name": "Sevilla FC", "shortName": "Sevilla" },
                  "awayTeam": { "name": "Real Betis Balompié", "shortName": "Betis" },
                  "score": { "fullTime": { "home": null, "away": null } } },
                { "id": 1003, "utcDate": "2026-08-23T00:00:00Z", "status": "SCHEDULED", "matchday": 2,
                  "homeTeam": { "name": "Valencia CF", "shortName": null },
                  "awayTeam": { "name": "Villarreal CF", "shortName": "Villarreal" },
                  "score": { "fullTime": { "home": null, "away": null } } },
                { "id": 1004, "utcDate": "2026-08-23T18:00:00Z", "status": "SOMETHING_NEW", "matchday": 2,
                  "homeTeam": { "name": "Girona FC", "shortName": "Girona" },
                  "awayTeam": { "name": "RCD Mallorca", "shortName": "Mallorca" },
                  "score": { "fullTime": { "home": null, "away": null } } }
              ]
            }
            """;

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    void mapsMatchesAndStatusesFromFootballData() {
        FootballDataOrgProvider provider = new FootballDataOrgProvider(builder, properties("test-token"));
        server.expect(requestTo(URL))
                .andExpect(header("X-Auth-Token", "test-token"))
                .andRespond(withSuccess(JSON, MediaType.APPLICATION_JSON));

        List<ExternalMatch> matches = provider.fetchMatches("PD", "2026");

        assertThat(matches).hasSize(3);   // el estado desconocido se omite

        ExternalMatch finished = matches.get(0);
        assertThat(finished.externalId()).isEqualTo("1001");
        assertThat(finished.status()).isEqualTo(FixtureStatus.FINISHED);
        assertThat(finished.homeTeam()).isEqualTo("Barça");
        assertThat(finished.homeGoals()).isEqualTo(2);
        assertThat(finished.kickoffAt()).isEqualTo(Instant.parse("2026-08-15T17:00:00Z"));

        assertThat(matches.get(1).status()).isEqualTo(FixtureStatus.SCHEDULED);
        assertThat(matches.get(1).kickoffConfirmed()).isTrue();

        ExternalMatch provisional = matches.get(2);
        assertThat(provisional.kickoffConfirmed()).isFalse();
        assertThat(provisional.homeTeam()).isEqualTo("Valencia CF");   // sin shortName, usa name

        server.verify();
    }

    @Test
    void wrapsHttpErrorsIntoProviderUnavailable() {
        FootballDataOrgProvider provider = new FootballDataOrgProvider(builder, properties("test-token"));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.fetchMatches("PD", "2026"))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void requiresToken() {
        FootballDataOrgProvider provider = new FootballDataOrgProvider(builder, properties(""));

        assertThatThrownBy(() -> provider.fetchMatches("PD", "2026"))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining("token");
    }

    private static FootballDataProperties properties(String token) {
        return new FootballDataProperties("https://api.football-data.org/v4", token,
                Duration.ofSeconds(5), Duration.ofSeconds(10));
    }
}