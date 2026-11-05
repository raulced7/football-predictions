package com.raulcedroj.football_predictions.standings.dto;

public record StandingEntry(int position, Long userId, String displayName, int points, int exactHits) {
}