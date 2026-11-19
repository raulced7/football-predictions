package com.raulcedroj.football_predictions.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** @param url ruta de la app que se abre al pulsar (opcional, por defecto la jornada) */
public record PushBroadcastRequest(
        @NotBlank @Size(max = 80) String title,
        @NotBlank @Size(max = 250) String body,
        @Size(max = 200) @Pattern(regexp = "/.*", message = "debe empezar por /") String url) {
}
