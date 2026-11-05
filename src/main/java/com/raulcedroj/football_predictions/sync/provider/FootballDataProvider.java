package com.raulcedroj.football_predictions.sync.provider;

import java.util.List;

public interface FootballDataProvider {

    List<ExternalMatch> fetchMatches(String competitionExternalId, String seasonExternalId);
}