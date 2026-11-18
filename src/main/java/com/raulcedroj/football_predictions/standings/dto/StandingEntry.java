package com.raulcedroj.football_predictions.standings.dto;

public record StandingEntry(int position, Long userId, String displayName, String avatarUrl, int points,
                            int exactHits) {
}