package com.raulcedroj.football_predictions.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(@NotBlank @Size(max = 50) String name, @NotNull Long seasonId) {
}