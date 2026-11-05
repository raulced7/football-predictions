package com.raulcedroj.football_predictions.competition.dto;

import com.raulcedroj.football_predictions.competition.RoundEntity;
import java.time.Instant;

public record RoundResponse(Long id, int sequence, String name, Instant startsAt) {

    public static RoundResponse from(RoundEntity round) {
        return new RoundResponse(round.getId(), round.getSequence(), round.getName(), round.getStartsAt());
    }
}