package com.raulcedroj.football_predictions.standings.dto;

import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;
import java.util.List;

public record GroupFixturePredictions(
        FixtureResponse fixture,
        boolean revealed,
        int predictedCount,
        List<MemberPrediction> predictions
) {
}