package com.raulcedroj.football_predictions.notification;

/** Datos de una suscripción del navegador: a dónde enviar y con qué claves cifrar. */
public record PushTarget(String endpoint, String p256dh, String auth) {
}
