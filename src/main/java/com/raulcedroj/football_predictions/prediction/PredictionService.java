package com.raulcedroj.football_predictions.prediction;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.competition.RoundRepository;
import com.raulcedroj.football_predictions.fixture.FixtureEntity;
import com.raulcedroj.football_predictions.fixture.FixtureRepository;
import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;
import com.raulcedroj.football_predictions.prediction.dto.PredictionRequest;
import com.raulcedroj.football_predictions.prediction.dto.PredictionResponse;
import com.raulcedroj.football_predictions.prediction.dto.RoundPredictionItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final FixtureRepository fixtureRepository;
    private final RoundRepository roundRepository;
    private final Clock clock;

    public PredictionService(PredictionRepository predictionRepository,
                             FixtureRepository fixtureRepository,
                             RoundRepository roundRepository,
                             Clock clock) {
        this.predictionRepository = predictionRepository;
        this.fixtureRepository = fixtureRepository;
        this.roundRepository = roundRepository;
        this.clock = clock;
    }

    @Transactional
    public PredictionResponse save(Long userId, Long fixtureId, PredictionRequest request) {
        FixtureEntity fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new NotFoundException("No existe el partido " + fixtureId));

        if (!fixture.isOpenForPredictions(Instant.now(clock))) {
            throw new BusinessRuleException("El partido ya ha empezado o no admite pronósticos");
        }

        predictionRepository.upsert(userId, fixtureId, request.homeGoals(), request.awayGoals());

        return predictionRepository.findByUserIdAndFixtureId(userId, fixtureId)
                .map(PredictionResponse::from)
                .orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<RoundPredictionItem> findMyRound(Long userId, Long roundId) {
        if (!roundRepository.existsById(roundId)) {
            throw new NotFoundException("No existe la jornada " + roundId);
        }

        List<FixtureEntity> fixtures = fixtureRepository.findByScoringRoundIdOrderByKickoffAtAsc(roundId);
        List<Long> fixtureIds = fixtures.stream().map(FixtureEntity::getId).toList();

        Map<Long, PredictionEntity> predictionsByFixture = predictionRepository
                .findByUserIdAndFixtureIdIn(userId, fixtureIds).stream()
                .collect(Collectors.toMap(p -> p.getFixture().getId(), Function.identity()));

        Instant now = Instant.now(clock);
        return fixtures.stream()
                .map(fixture -> {
                    PredictionEntity prediction = predictionsByFixture.get(fixture.getId());
                    return new RoundPredictionItem(
                            FixtureResponse.from(fixture),
                            fixture.isOpenForPredictions(now),
                            prediction == null ? null : PredictionResponse.from(prediction));
                })
                .toList();
    }
}