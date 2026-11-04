package com.raulcedroj.football_predictions.fixture.dto;

import com.raulcedroj.football_predictions.fixture.FixtureEntity;
import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import java.time.Instant;

public record FixtureResponse(
        Long id,
        Long roundId,
        String roundName,
        Long scoringRoundId,
        String homeTeam,
        String awayTeam,
        Instant kickoffAt,
        boolean kickoffConfirmed,
        FixtureStatus status,
        Integer homeGoals,
        Integer awayGoals
) {
    public static FixtureResponse from(FixtureEntity fixture) {
        return new FixtureResponse(
                fixture.getId(),
                fixture.getRound().getId(),
                fixture.getRound().getName(),
                fixture.getScoringRound().getId(),
                fixture.getHomeTeam(),
                fixture.getAwayTeam(),
                fixture.getKickoffAt(),
                fixture.isKickoffConfirmed(),
                fixture.getStatus(),
                fixture.getHomeGoals(),
                fixture.getAwayGoals());
    }
}