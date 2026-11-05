package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.competition.*;
import com.raulcedroj.football_predictions.fixture.*;
import com.raulcedroj.football_predictions.sync.provider.ExternalMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FixtureSyncWriter {

    private static final Logger log = LoggerFactory.getLogger(FixtureSyncWriter.class);

    private final FixtureRepository fixtureRepository;
    private final RoundRepository roundRepository;
    private final ScoringRoundResolver scoringRoundResolver;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final SeasonRepository seasonRepository;

    public FixtureSyncWriter(FixtureRepository fixtureRepository, RoundRepository roundRepository,
                             ScoringRoundResolver scoringRoundResolver, ApplicationEventPublisher events,
                             Clock clock, SeasonRepository seasonRepository) {
        this.fixtureRepository = fixtureRepository;
        this.roundRepository = roundRepository;
        this.scoringRoundResolver = scoringRoundResolver;
        this.events = events;
        this.clock = clock;
        this.seasonRepository = seasonRepository;
    }

    @Transactional
    public SyncReport apply(Long seasonId, List<ExternalMatch> matches) {
        if (matches.isEmpty()) {
            return new SyncReport(0, 0, 0, 0, updateSeasonStatus(seasonId));
        }

        Map<Integer, RoundEntity> roundsBySequence = roundRepository.findBySeasonIdOrderBySequenceAsc(seasonId)
                .stream().collect(Collectors.toMap(RoundEntity::getSequence, Function.identity()));

        Map<String, FixtureEntity> existing = fixtureRepository
                .findByExternalIdIn(matches.stream().map(ExternalMatch::externalId).toList())
                .stream().collect(Collectors.toMap(FixtureEntity::getExternalId, Function.identity()));

        int created = 0, updated = 0, unchanged = 0, skipped = 0;

        for (ExternalMatch match : matches) {
            try {
                FixtureEntity fixture = existing.get(match.externalId());
                if (fixture == null) {
                    RoundEntity round = roundsBySequence.get(match.matchday());
                    if (round == null) {
                        log.warn("Skipping match {}: matchday {} does not exist", match.externalId(), match.matchday());
                        skipped++;
                        continue;
                    }
                    fixture = fixtureRepository.save(new FixtureEntity(round, match.homeTeam(), match.awayTeam(),
                            match.kickoffAt(), match.kickoffConfirmed(), match.externalId()));
                    applyStatus(fixture, match);
                    created++;
                } else if (update(fixture, match)) {
                    updated++;
                } else {
                    unchanged++;
                }
            } catch (BusinessRuleException e) {
                log.warn("Could not apply match {}: {}", match.externalId(), e.getMessage());
                skipped++;
            }
        }

        roundRepository.recomputeStarts(seasonId);
        return new SyncReport(created, updated, unchanged, skipped, updateSeasonStatus(seasonId));
    }

    private boolean update(FixtureEntity fixture, ExternalMatch match) {
        boolean changed = false;

        boolean scheduleChanged = !fixture.getKickoffAt().equals(match.kickoffAt())
                || fixture.isKickoffConfirmed() != match.kickoffConfirmed();
        boolean reopening = fixture.getStatus() == FixtureStatus.POSTPONED
                && match.status() != FixtureStatus.POSTPONED
                && match.status() != FixtureStatus.CANCELLED;

        if ((scheduleChanged || reopening) && fixture.canBeRescheduled()) {
            fixture.reschedule(match.kickoffAt(), match.kickoffConfirmed(),
                    scoringRoundResolver.resolve(fixture.getRound(), match.kickoffAt()));
            changed = true;
        }

        return applyStatus(fixture, match) || changed;
    }

    private boolean applyStatus(FixtureEntity fixture, ExternalMatch match) {
        FixtureStatus current = fixture.getStatus();

        switch (match.status()) {
            case LIVE -> {
                if (current == FixtureStatus.SCHEDULED || current == FixtureStatus.SUSPENDED) {
                    fixture.markLive();
                    return true;
                }
            }
            case FINISHED -> {
                if (match.homeGoals() == null || match.awayGoals() == null) {
                    return false;
                }
                if (current == FixtureStatus.FINISHED && fixture.hasResult(match.homeGoals(), match.awayGoals())) {
                    return false;
                }
                fixture.finish(match.homeGoals(), match.awayGoals());
                events.publishEvent(new FixtureFinishedEvent(fixture.getId(), match.homeGoals(), match.awayGoals()));
                return true;
            }
            case POSTPONED -> {
                if (current == FixtureStatus.SCHEDULED) {
                    fixture.postpone();
                    return true;
                }
            }
            case SUSPENDED -> {
                if (current == FixtureStatus.SCHEDULED || current == FixtureStatus.LIVE) {
                    fixture.suspend(Instant.now(clock));
                    return true;
                }
            }
            case CANCELLED -> {
                if (current != FixtureStatus.CANCELLED) {
                    fixture.cancel();
                    events.publishEvent(new FixtureCancelledEvent(fixture.getId()));
                    return true;
                }
            }
            case SCHEDULED -> {
                // Un aplazado que vuelve a tener fecha ya se ha reabierto con reschedule()
            }
        }
        return false;
    }

    private SeasonStatus updateSeasonStatus(Long seasonId) {
        SeasonEntity season = seasonRepository.findById(seasonId).orElseThrow();

        boolean started = fixtureRepository.existsByRoundSeasonIdAndStatusIn(seasonId,
                List.of(FixtureStatus.LIVE, FixtureStatus.FINISHED, FixtureStatus.SUSPENDED));
        if (started) {
            season.markInProgress();
        }

        long total = fixtureRepository.countByRoundSeasonId(seasonId);
        long pending = fixtureRepository.countByRoundSeasonIdAndStatusNotIn(seasonId,
                List.of(FixtureStatus.FINISHED, FixtureStatus.CANCELLED));
        if (season.getStatus() == SeasonStatus.IN_PROGRESS && total > 0 && pending == 0) {
            season.markFinished();
        }
        return season.getStatus();
    }
}