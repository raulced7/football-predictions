package com.raulcedroj.football_predictions.email;

public record EmailMessage(String to, String subject, String text, String html) {
}