package com.raulcedroj.football_predictions.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinGroupRequest(@NotBlank @Size(max = 12) String inviteCode) {
}