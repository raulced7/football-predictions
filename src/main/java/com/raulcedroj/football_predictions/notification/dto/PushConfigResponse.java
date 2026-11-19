package com.raulcedroj.football_predictions.notification.dto;

/** Si los avisos no están configurados en el servidor, el front ni siquiera los ofrece. */
public record PushConfigResponse(boolean enabled, String publicKey) {
}
