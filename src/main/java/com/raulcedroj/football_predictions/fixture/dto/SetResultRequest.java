package com.raulcedroj.football_predictions.fixture.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetResultRequest(
        @NotNull @Min(0) @Max(99) Integer homeGoals,
        @NotNull @Min(0) @Max(99) Integer awayGoals
) {
}