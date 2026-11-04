package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.competition.RoundEntity;
import com.raulcedroj.football_predictions.competition.RoundRepository;
import com.raulcedroj.football_predictions.fixture.dto.CreateFixtureRequest;
import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;
import com.raulcedroj.football_predictions.fixture.dto.SetResultRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final RoundRepository roundRepository;

    public FixtureService(FixtureRepository fixtureRepository, RoundRepository roundRepository) {
        this.fixtureRepository = fixtureRepository;
        this.roundRepository = roundRepository;
    }

    @Transactional
    public FixtureResponse create(CreateFixtureRequest request) {
        if (request.homeTeam().trim().equalsIgnoreCase(request.awayTeam().trim())) {
            throw new BusinessRuleException("El equipo local y el visitante no pueden ser el mismo");
        }
        RoundEntity round = roundRepository.findById(request.roundId())
                .orElseThrow(() -> new NotFoundException("No existe la jornada " + request.roundId()));

        FixtureEntity fixture = fixtureRepository.save(new FixtureEntity(round,
                request.homeTeam().trim(), request.awayTeam().trim(),
                request.kickoffAt(), request.kickoffConfirmed()));

        return FixtureResponse.from(fixture);
    }

    @Transactional
    public FixtureResponse setResult(Long fixtureId, SetResultRequest request) {
        FixtureEntity fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new NotFoundException("No existe el partido " + fixtureId));

        fixture.finish(request.homeGoals(), request.awayGoals());
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
}