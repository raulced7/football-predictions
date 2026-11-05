package com.raulcedroj.football_predictions.fixture.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ScheduleFixtureRequest(@NotNull Instant kickoffAt, boolean kickoffConfirmed) {
}