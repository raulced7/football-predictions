package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.competition.SeasonStatus;

public record SyncReport(int created, int updated, int unchanged, int skipped, SeasonStatus seasonStatus) {
}