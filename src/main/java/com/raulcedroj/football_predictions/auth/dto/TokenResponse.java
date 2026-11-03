package com.raulcedroj.football_predictions.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}