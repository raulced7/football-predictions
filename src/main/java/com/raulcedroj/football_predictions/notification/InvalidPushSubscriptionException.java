package com.raulcedroj.football_predictions.notification;

public class InvalidPushSubscriptionException extends RuntimeException {

    public InvalidPushSubscriptionException(String message) {
        super(message);
    }
}
