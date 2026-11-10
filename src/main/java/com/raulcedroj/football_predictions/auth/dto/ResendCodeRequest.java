package com.raulcedroj.football_predictions.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendCodeRequest(@NotBlank @Email String email) {
}