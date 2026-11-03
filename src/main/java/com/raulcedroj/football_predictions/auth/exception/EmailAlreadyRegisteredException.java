package com.raulcedroj.football_predictions.auth.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() {
        super("Ya existe una cuenta con ese email");
    }
}