package com.raulcedroj.football_predictions.sync;

public record SyncReport(int created, int updated, int unchanged, int skipped) {
}