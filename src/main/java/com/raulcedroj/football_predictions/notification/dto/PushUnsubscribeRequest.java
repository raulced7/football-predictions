package com.raulcedroj.football_predictions.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record PushUnsubscribeRequest(@NotBlank String endpoint) {
}
