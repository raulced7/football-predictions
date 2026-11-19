package com.raulcedroj.football_predictions.notification.dto;

/** @param sent a cuántos dispositivos del usuario ha llegado (0: no tiene avisos activados en ninguno) */
public record PushTestResponse(int sent) {
}
