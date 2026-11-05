package com.raulcedroj.football_predictions.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.sync")
public record SyncProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("PT24H") Duration liveLookback,
        @DefaultValue("PT5M") Duration liveLookahead
) {
}