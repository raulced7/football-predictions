package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.competition.RoundEntity;
import com.raulcedroj.football_predictions.competition.RoundRepository;
import com.raulcedroj.football_predictions.fixture.dto.CreateFixtureRequest;
import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;
import com.raulcedroj.football_predictions.fixture.dto.ScheduleFixtureRequest;
import com.raulcedroj.football_predictions.fixture.dto.SetResultRequest;
import com.raulcedroj.football_predictions.team.TeamEntity;
import com.raulcedroj.football_predictions.team.TeamService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final RoundRepository roundRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final ScoringRoundResolver scoringRoundResolver;
    private final TeamService teamService;

    public FixtureService(FixtureRepository fixtureRepository, RoundRepository roundRepository, ApplicationEventPublisher eventPublisher,
                          Clock clock, ScoringRoundResolver scoringRoundResolver, TeamService teamService) {
        this.fixtureRepository = fixtureRepository;
        this.roundRepository = roundRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.scoringRoundResolver = scoringRoundResolver;
        this.teamService = teamService;
    }

    @Transactional
    public FixtureResponse create(CreateFixtureRequest request) {
        if (request.homeTeam().trim().equalsIgnoreCase(request.awayTeam().trim())) {
            throw new BusinessRuleException("El equipo local y el visitante no pueden ser el mismo");
        }
        RoundEntity round = roundRepository.findById(request.roundId())
                .orElseThrow(() -> new NotFoundException("No existe la jornada " + request.roundId()));

        TeamEntity homeTeam = teamService.findOrCreate(request.homeTeam());
        TeamEntity awayTeam = teamService.findOrCreate(request.awayTeam());

        FixtureEntity fixture = fixtureRepository.save(new FixtureEntity(round, homeTeam, awayTeam,
                request.kickoffAt(), request.kickoffConfirmed()));

        roundRepository.recomputeStarts(round.getSeason().getId());

        return FixtureResponse.from(fixture);
    }

    @Transactional
    public FixtureResponse setResult(Long fixtureId, SetResultRequest request) {
        FixtureEntity fixture = getFixture(fixtureId);

        fixture.finish(request.homeGoals(), request.awayGoals());
        eventPublisher.publishEvent(
                new FixtureFinishedEvent(fixture.getId(), request.homeGoals(), request.awayGoals()));

        return FixtureResponse.from(fixture);
    }

    @Transactional(readOnly = true)
    public List<FixtureResponse> findByRound(Long roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new NotFoundException("No existe la jornada " + roundId);
        }
        return fixtureRepository.findByScoringRoundIdOrderByKickoffAtAsc(roundId).stream()
                .map(FixtureResponse::from)
                .toList();
    }

    @Transactional
    public FixtureResponse postpone(Long fixtureId) {
        FixtureEntity fixture = getFixture(fixtureId);
        fixture.postpone();
        return FixtureResponse.from(fixture);
    }

    @Transactional
    public FixtureResponse reschedule(Long fixtureId, ScheduleFixtureRequest request) {
        FixtureEntity fixture = getFixture(fixtureId);
        RoundEntity officialRound = fixture.getRound();
        Long seasonId = officialRound.getSeason().getId();

        fixture.reschedule(request.kickoffAt(), request.kickoffConfirmed(),
                scoringRoundResolver.resolve(officialRound, request.kickoffAt()));

        roundRepository.recomputeStarts(seasonId);
        return FixtureResponse.from(fixture);
    }

    @Transactional
    public FixtureResponse suspend(Long fixtureId) {
        FixtureEntity fixture = getFixture(fixtureId);
        fixture.suspend(Instant.now(clock));
        return FixtureResponse.from(fixture);
    }

    @Transactional
    public FixtureResponse cancel(Long fixtureId) {
        FixtureEntity fixture = getFixture(fixtureId);
        fixture.cancel();
        eventPublisher.publishEvent(new FixtureCancelledEvent(fixture.getId()));
        return FixtureResponse.from(fixture);
    }

    private FixtureEntity getFixture(Long fixtureId) {
        return fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new NotFoundException("No existe el partido " + fixtureId));
    }
}