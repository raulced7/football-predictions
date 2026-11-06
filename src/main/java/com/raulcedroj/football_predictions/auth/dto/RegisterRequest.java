package com.raulcedroj.football_predictions.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} ._-]{1,18}[\\p{L}\\p{N}]$",
                message = "debe tener entre 3 y 20 caracteres: letras, números, espacios, puntos, guiones o _")
        String displayName,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}