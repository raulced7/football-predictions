package com.raulcedroj.football_predictions.sync.footballdata;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.football-data")
public record FootballDataProperties(
        @DefaultValue("https://api.football-data.org/v4") String baseUrl,
        String token,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout
) {
}