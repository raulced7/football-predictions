package com.raulcedroj.football_predictions.fixture;

public record FixtureFinishedEvent(Long fixtureId, int homeGoals, int awayGoals) {
}