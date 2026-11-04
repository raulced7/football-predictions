package com.raulcedroj.football_predictions.group.dto;

import com.raulcedroj.football_predictions.group.FriendGroupEntity;

public record GroupResponse(
        Long id,
        String name,
        String inviteCode,
        Long seasonId,
        String competitionName,
        String seasonName,
        Long ownerId,
        long memberCount,
        boolean readOnly
) {
    public static GroupResponse from(FriendGroupEntity group, long memberCount) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                group.getSeason().getId(),
                group.getSeason().getCompetition().getName(),
                group.getSeason().getName(),
                group.getOwnerId(),
                memberCount,
                group.isReadOnly());
    }
}