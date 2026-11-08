package com.raulcedroj.football_predictions.team.dto;

import com.raulcedroj.football_predictions.team.TeamEntity;

public record TeamResponse(
        Long id,
        String name,
        String shortName,
        String tla,
        String crestUrl
) {
    public static TeamResponse from(TeamEntity team) {
        return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getTla(), team.getCrestUrl());
    }
}