package com.raulcedroj.football_predictions.sync.footballdata;

import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import com.raulcedroj.football_predictions.sync.footballdata.FootballDataMatchesResponse.Match;
import com.raulcedroj.football_predictions.sync.footballdata.FootballDataMatchesResponse.ScoreLine;
import com.raulcedroj.football_predictions.sync.footballdata.FootballDataMatchesResponse.Team;
import com.raulcedroj.football_predictions.sync.provider.ExternalMatch;
import com.raulcedroj.football_predictions.sync.provider.ExternalTeam;
import com.raulcedroj.football_predictions.sync.provider.FootballDataProvider;
import com.raulcedroj.football_predictions.sync.provider.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

@Component
public class FootballDataOrgProvider implements FootballDataProvider {

    private static final Logger log = LoggerFactory.getLogger(FootballDataOrgProvider.class);

    private final RestClient restClient;
    private final FootballDataProperties properties;

    @Autowired
    public FootballDataOrgProvider(FootballDataProperties properties) {
        this(RestClient.builder().requestFactory(requestFactory(properties)), properties);
    }

    FootballDataOrgProvider(RestClient.Builder builder, FootballDataProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-Auth-Token", properties.token() == null ? "" : properties.token())
                .build();
    }

    @Override
    public List<ExternalMatch> fetchMatches(String competitionExternalId, String seasonExternalId) {
        if (!StringUtils.hasText(properties.token())) {
            throw new ProviderUnavailableException("No se ha configurado el token de football-data.org", null);
        }

        FootballDataMatchesResponse response;
        try {
            response = restClient.get()
                    .uri("/competitions/{competition}/matches?season={season}",
                            competitionExternalId, seasonExternalId)
                    .retrieve()
                    .body(FootballDataMatchesResponse.class);
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("football-data.org no está disponible: " + e.getMessage(), e);
        }

        if (response == null || response.matches() == null) {
            return List.of();
        }
        return response.matches().stream()
                .map(this::toExternalMatch)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<ExternalMatch> toExternalMatch(Match match) {
        MappedStatus status = mapStatus(match.status());
        ExternalTeam homeTeam = toExternalTeam(match.homeTeam());
        ExternalTeam awayTeam = toExternalTeam(match.awayTeam());
        if (status == null || match.matchday() == null || match.utcDate() == null
                || homeTeam == null || awayTeam == null) {
            log.warn("Skipping match {} with unsupported data (status={}, matchday={})",
                    match.id(), match.status(), match.matchday());
            return Optional.empty();
        }

        ScoreLine score = match.score() == null ? null
                : match.score().regularTime() != null ? match.score().regularTime() : match.score().fullTime();

        return Optional.of(new ExternalMatch(
                String.valueOf(match.id()),
                match.matchday(),
                match.utcDate(),
                status.kickoffConfirmed(),
                status.status(),
                homeTeam,
                awayTeam,
                score == null ? null : score.home(),
                score == null ? null : score.away()));
    }

    static MappedStatus mapStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "SCHEDULED" -> new MappedStatus(FixtureStatus.SCHEDULED, false);
            case "TIMED" -> new MappedStatus(FixtureStatus.SCHEDULED, true);
            case "IN_PLAY", "PAUSED", "LIVE", "EXTRA_TIME", "PENALTY_SHOOTOUT" ->
                    new MappedStatus(FixtureStatus.LIVE, true);
            case "FINISHED", "AWARDED" -> new MappedStatus(FixtureStatus.FINISHED, true);
            case "POSTPONED" -> new MappedStatus(FixtureStatus.POSTPONED, false);
            case "SUSPENDED" -> new MappedStatus(FixtureStatus.SUSPENDED, true);
            case "CANCELLED" -> new MappedStatus(FixtureStatus.CANCELLED, false);
            default -> null;
        };
    }

    /** Sin id o sin nombre no podemos identificar al equipo (por ejemplo, un cruce aún por decidir). */
    private static ExternalTeam toExternalTeam(Team team) {
        if (team == null || team.id() == null || !StringUtils.hasText(team.name())) {
            return null;
        }
        return new ExternalTeam(
                String.valueOf(team.id()),
                team.name().trim(),
                textOrNull(team.shortName()),
                textOrNull(team.tla()),
                textOrNull(team.crest()));
    }

    private static String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static ClientHttpRequestFactory requestFactory(FootballDataProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }

    record MappedStatus(FixtureStatus status, boolean kickoffConfirmed) {
    }
}