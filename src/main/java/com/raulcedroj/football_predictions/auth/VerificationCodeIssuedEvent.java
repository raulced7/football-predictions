package com.raulcedroj.football_predictions.auth;

/** Se ha generado un código de verificación y hay que enviarlo por correo. */
public record VerificationCodeIssuedEvent(String email, String displayName, String code) {
}