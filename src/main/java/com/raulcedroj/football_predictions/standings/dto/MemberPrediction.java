package com.raulcedroj.football_predictions.standings.dto;

public record MemberPrediction(Long userId, String displayName, int homeGoals, int awayGoals, Integer points) {
}