package com.raulcedroj.football_predictions.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        @DefaultValue("Golify <no-reply@golifyapp.com>") String from,
        String resendApiKey,
        @DefaultValue("https://api.resend.com") String resendBaseUrl
) {
}