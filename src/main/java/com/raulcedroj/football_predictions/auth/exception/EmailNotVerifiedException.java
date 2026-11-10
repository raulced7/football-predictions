package com.raulcedroj.football_predictions.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Tienes que verificar tu email antes de entrar");
    }
}