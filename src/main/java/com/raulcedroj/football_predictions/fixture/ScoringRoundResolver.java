package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.competition.RoundEntity;
import com.raulcedroj.football_predictions.competition.RoundRepository;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class ScoringRoundResolver {

    private final FixtureRepository fixtureRepository;
    private final RoundRepository roundRepository;

    public ScoringRoundResolver(FixtureRepository fixtureRepository, RoundRepository roundRepository) {
        this.fixtureRepository = fixtureRepository;
        this.roundRepository = roundRepository;
    }

    public RoundEntity resolve(RoundEntity officialRound, Instant kickoffAt) {
        Long roundId = fixtureRepository.findScoringRoundIdFor(
                        officialRound.getSeason().getId(), officialRound.getSequence(), kickoffAt)
                .orElse(officialRound.getId());
        return roundRepository.getReferenceById(roundId);
    }
}