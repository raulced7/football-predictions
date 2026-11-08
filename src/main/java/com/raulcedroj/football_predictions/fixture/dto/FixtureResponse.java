package com.raulcedroj.football_predictions.fixture.dto;

import com.raulcedroj.football_predictions.fixture.FixtureEntity;
import com.raulcedroj.football_predictions.fixture.FixtureStatus;
import com.raulcedroj.football_predictions.team.dto.TeamResponse;

import java.time.Instant;

public record FixtureResponse(
        Long id,
        Long roundId,
        String roundName,
        Long scoringRoundId,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
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
                TeamResponse.from(fixture.getHomeTeam()),
                TeamResponse.from(fixture.getAwayTeam()),
                fixture.getKickoffAt(),
                fixture.isKickoffConfirmed(),
                fixture.getStatus(),
                fixture.getHomeGoals(),
                fixture.getAwayGoals());
    }
}