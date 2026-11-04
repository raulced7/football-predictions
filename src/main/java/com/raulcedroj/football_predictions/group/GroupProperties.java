package com.raulcedroj.football_predictions.group;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.groups")
public record GroupProperties(@DefaultValue("50") int maxMembers,
                              @DefaultValue("10") int maxOwnedPerUser) {
}