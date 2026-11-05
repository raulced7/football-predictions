package com.raulcedroj.football_predictions.sync.footballdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record FootballDataMatchesResponse(List<Match> matches) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Match(long id, Instant utcDate, String status, Integer matchday,
                 Team homeTeam, Team awayTeam, Score score) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Team(String name, String shortName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Score(ScoreLine fullTime, ScoreLine regularTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScoreLine(Integer home, Integer away) {
    }
}