package com.raulcedroj.football_predictions.prediction.dto;

import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;

public record RoundPredictionItem(
        FixtureResponse fixture,
        boolean open,
        PredictionResponse prediction
) {
}