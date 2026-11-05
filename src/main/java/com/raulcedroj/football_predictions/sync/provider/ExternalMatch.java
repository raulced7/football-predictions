package com.raulcedroj.football_predictions.sync.provider;

import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import java.time.Instant;

public record ExternalMatch(
        String externalId,
        int matchday,
        Instant kickoffAt,
        boolean kickoffConfirmed,
        FixtureStatus status,
        String homeTeam,
        String awayTeam,
        Integer homeGoals,
        Integer awayGoals
) {
}