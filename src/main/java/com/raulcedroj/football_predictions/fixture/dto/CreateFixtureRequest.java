package com.raulcedroj.football_predictions.fixture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateFixtureRequest(
        @NotNull Long roundId,
        @NotBlank @Size(max = 100) String homeTeam,
        @NotBlank @Size(max = 100) String awayTeam,
        @NotNull Instant kickoffAt,
        boolean kickoffConfirmed
) {
}