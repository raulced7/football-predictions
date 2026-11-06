package com.raulcedroj.football_predictions.auth.exception;

public class DisplayNameTakenException extends RuntimeException {
    public DisplayNameTakenException() {
        super("Ese nombre de usuario ya está en uso");
    }
}