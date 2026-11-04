package com.raulcedroj.football_predictions.prediction.dto;

import com.raulcedroj.football_predictions.prediction.PredictionEntity;
import java.time.Instant;

public record PredictionResponse(
        Long fixtureId,
        int homeGoals,
        int awayGoals,
        Integer points,
        Instant updatedAt
) {
    public static PredictionResponse from(PredictionEntity prediction) {
        return new PredictionResponse(
                prediction.getFixture().getId(),
                prediction.getHomeGoals(),
                prediction.getAwayGoals(),
                prediction.getPoints(),
                prediction.getUpdatedAt());
    }
}