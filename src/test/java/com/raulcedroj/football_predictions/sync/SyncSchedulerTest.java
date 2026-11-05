package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.competition.CompetitionEntity;
import com.raulcedroj.football_predictions.competition.SeasonEntity;
import com.raulcedroj.football_predictions.competition.SeasonRepository;
import com.raulcedroj.football_predictions.competition.SeasonStatus;
import com.raulcedroj.football_predictions.fixture.FixtureRepository;
import com.raulcedroj.football_predictions.sync.provider.ProviderUnavailableException;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SyncSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-11-08T20:00:00Z");

    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final FixtureRepository fixtureRepository = mock(FixtureRepository.class);
    private final CalendarSyncService syncService = mock(CalendarSyncService.class);

    private final SyncScheduler scheduler = new SyncScheduler(seasonRepository, fixtureRepository, syncService,
            new SyncProperties(true, Duration.ofHours(24), Duration.ofMinutes(5)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void syncsOnlySeasonsWithMatchesInPlay() {
        SeasonEntity withMatches = season(1L);
        SeasonEntity withoutMatches = season(2L);
        when(seasonRepository.findByStatusNotOrderByStartDateDesc(SeasonStatus.FINISHED))
                .thenReturn(List.of(withMatches, withoutMatches));
        when(fixtureRepository.existsByRoundSeasonIdAndStatusInAndKickoffAtBetweenAndExternalIdIsNotNull(
                eq(1L), anyCollection(), eq(NOW.minus(Duration.ofHours(24))), eq(NOW.plus(Duration.ofMinutes(5)))))
                .thenReturn(true);

        scheduler.syncMatchesInPlay();

        verify(syncService).syncSeason(1L);
        verify(syncService, never()).syncSeason(2L);
    }

    @Test
    void failureInOneSeasonDoesNotStopTheOthers() {
        SeasonEntity first = season(1L);
        SeasonEntity second = season(2L);
        when(seasonRepository.findByStatusNotOrderByStartDateDesc(SeasonStatus.FINISHED))
                .thenReturn(List.of(first, second));
        when(syncService.syncSeason(1L)).thenThrow(new ProviderUnavailableException("down", null));

        scheduler.syncCalendars();

        verify(syncService).syncSeason(1L);
        verify(syncService).syncSeason(2L);
    }

    @Test
    void ignoresSeasonsNotLinkedToTheProvider() {
        SeasonEntity unlinked = season(3L);
        when(unlinked.getExternalId()).thenReturn(null);
        when(seasonRepository.findByStatusNotOrderByStartDateDesc(SeasonStatus.FINISHED))
                .thenReturn(List.of(unlinked));

        scheduler.syncCalendars();

        verify(syncService, never()).syncSeason(any());
    }

    private static SeasonEntity season(Long id) {
        CompetitionEntity competition = mock(CompetitionEntity.class);
        when(competition.getExternalId()).thenReturn("PD");

        SeasonEntity season = mock(SeasonEntity.class);
        when(season.getId()).thenReturn(id);
        when(season.getExternalId()).thenReturn("2026");
        when(season.getCompetition()).thenReturn(competition);
        return season;
    }
}