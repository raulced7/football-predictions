package com.raulcedroj.football_predictions.competition.dto;

import com.raulcedroj.football_predictions.competition.SeasonEntity;
import com.raulcedroj.football_predictions.competition.SeasonStatus;

public record SeasonResponse(Long id, String competitionCode, String competitionName,
                             String name, SeasonStatus status) {

    public static SeasonResponse from(SeasonEntity season) {
        return new SeasonResponse(season.getId(), season.getCompetition().getCode(),
                season.getCompetition().getName(), season.getName(), season.getStatus());
    }
}