package com.raulcedroj.football_predictions.scoring;

import com.raulcedroj.football_predictions.fixture.FixtureCancelledEvent;
import com.raulcedroj.football_predictions.fixture.FixtureFinishedEvent;
import com.raulcedroj.football_predictions.prediction.PredictionEntity;
import com.raulcedroj.football_predictions.prediction.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final PredictionRepository predictionRepository;
    private final ScoringRules scoringRules;

    public ScoringService(PredictionRepository predictionRepository, ScoringRules scoringRules) {
        this.predictionRepository = predictionRepository;
        this.scoringRules = scoringRules;
    }

    @EventListener
    public void onFixtureFinished(FixtureFinishedEvent event) {
        List<PredictionEntity> predictions = predictionRepository.findByFixtureId(event.fixtureId());

        for (PredictionEntity prediction : predictions) {
            prediction.assignPoints(scoringRules.points(
                    prediction.getHomeGoals(), prediction.getAwayGoals(),
                    event.homeGoals(), event.awayGoals()));
        }

        log.info("Scored {} predictions for fixture {}", predictions.size(), event.fixtureId());
    }

    @EventListener
    public void onFixtureCancelled(FixtureCancelledEvent event) {
        int cleared = predictionRepository.clearPoints(event.fixtureId());
        log.info("Cleared points of {} predictions for cancelled fixture {}", cleared, event.fixtureId());
    }
}