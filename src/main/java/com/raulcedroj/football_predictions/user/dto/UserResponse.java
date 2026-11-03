package com.raulcedroj.football_predictions.user.dto;

import com.raulcedroj.football_predictions.user.UserEntity;

public record UserResponse(Long id, String email, String displayName) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}