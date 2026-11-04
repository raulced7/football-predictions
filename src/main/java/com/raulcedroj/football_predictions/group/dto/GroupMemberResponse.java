package com.raulcedroj.football_predictions.group.dto;

import java.time.Instant;

public record GroupMemberResponse(Long userId, String displayName, Instant joinedAt) {
}