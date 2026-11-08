package com.raulcedroj.football_predictions.sync.provider;

public record ExternalTeam(
        String externalId,
        String name,
        String shortName,
        String tla,
        String crestUrl
) {
}