package com.raulcedroj.football_predictions.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mismo formato que {@code PushSubscription.toJSON()} en el navegador. */
public record PushSubscriptionRequest(
        @NotBlank @Size(max = 1000) String endpoint,
        @NotNull @Valid Keys keys) {

    public record Keys(@NotBlank @Size(max = 200) String p256dh, @NotBlank @Size(max = 50) String auth) {
    }
}
