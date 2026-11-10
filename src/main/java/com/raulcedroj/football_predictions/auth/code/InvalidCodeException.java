package com.raulcedroj.football_predictions.auth.code;

public class InvalidCodeException extends RuntimeException {

    public InvalidCodeException(String message) {
        super(message);
    }
}