package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.competition.SeasonEntity;
import com.raulcedroj.football_predictions.competition.SeasonRepository;
import com.raulcedroj.football_predictions.competition.SeasonStatus;
import com.raulcedroj.football_predictions.fixture.FixtureRepository;
import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import com.raulcedroj.football_predictions.sync.provider.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SeasonRepository seasonRepository;
    private final FixtureRepository fixtureRepository;
    private final CalendarSyncService calendarSyncService;
    private final SyncProperties properties;
    private final Clock clock;

    public SyncScheduler(SeasonRepository seasonRepository, FixtureRepository fixtureRepository,
                         CalendarSyncService calendarSyncService, SyncProperties properties, Clock clock) {
        this.seasonRepository = seasonRepository;
        this.fixtureRepository = fixtureRepository;
        this.calendarSyncService = calendarSyncService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.sync.calendar-cron:0 30 4 * * *}", zone = "Europe/Madrid")
    public void syncCalendars() {
        for (SeasonEntity season : activeSeasons()) {
            syncSafely(season, "calendar");
        }
    }

    @Scheduled(fixedDelayString = "${app.sync.live-check-interval:PT5M}", initialDelayString = "PT1M")
    public void syncMatchesInPlay() {
        Instant now = Instant.now(clock);
        for (SeasonEntity season : activeSeasons()) {
            if (hasMatchesInPlayWindow(season.getId(), now)) {
                syncSafely(season, "live");
            } else {
                log.debug("No matches in play for season {}, skipping", season.getId());
            }
        }
    }

    private boolean hasMatchesInPlayWindow(Long seasonId, Instant now) {
        return fixtureRepository.existsByRoundSeasonIdAndStatusInAndKickoffAtBetweenAndExternalIdIsNotNull(
                seasonId,
                List.of(FixtureStatus.SCHEDULED, FixtureStatus.LIVE),
                now.minus(properties.liveLookback()),
                now.plus(properties.liveLookahead()));
    }

    private List<SeasonEntity> activeSeasons() {
        return seasonRepository.findByStatusNotOrderByStartDateDesc(SeasonStatus.FINISHED).stream()
                .filter(season -> season.getExternalId() != null
                        && season.getCompetition().getExternalId() != null)
                .toList();
    }

    private void syncSafely(SeasonEntity season, String trigger) {
        try {
            SyncReport report = calendarSyncService.syncSeason(season.getId());
            log.info("Scheduled {} sync of season {}: {}", trigger, season.getId(), report);
        } catch (ProviderUnavailableException e) {
            log.warn("Scheduled {} sync of season {} skipped: {}", trigger, season.getId(), e.getMessage());
        } catch (BusinessRuleException e) {
            log.info("Scheduled {} sync of season {} skipped: {}", trigger, season.getId(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Scheduled {} sync of season {} failed", trigger, season.getId(), e);
        }
    }
}