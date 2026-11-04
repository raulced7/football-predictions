package com.raulcedroj.football_predictions.group.dto;

import java.util.List;

public record GroupDetailResponse(GroupResponse group, List<GroupMemberResponse> members) {
}