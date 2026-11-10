package com.raulcedroj.football_predictions.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "debe tener 6 dígitos") String code
) {
}