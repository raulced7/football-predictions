package com.raulcedroj.football_predictions.auth;

import com.raulcedroj.football_predictions.auth.code.CodePurpose;

public record EmailCodeIssuedEvent(String email, String displayName, String code, CodePurpose purpose) {
}