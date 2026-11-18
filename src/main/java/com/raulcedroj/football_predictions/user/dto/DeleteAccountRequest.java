package com.raulcedroj.football_predictions.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Se pide la contraseña para que nadie pueda borrar la cuenta con un móvil desbloqueado. */
public record DeleteAccountRequest(@NotBlank String password) {
}
