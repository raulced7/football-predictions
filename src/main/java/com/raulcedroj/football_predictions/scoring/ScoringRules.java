package com.raulcedroj.football_predictions.scoring;

import org.springframework.stereotype.Component;

@Component
public class ScoringRules {

    public static final int EXACT_RESULT_POINTS = 3;
    static final int CORRECT_OUTCOME_POINTS = 1;

    public int points(int predictedHome, int predictedAway, int actualHome, int actualAway) {
        if (predictedHome == actualHome && predictedAway == actualAway) {
            return EXACT_RESULT_POINTS;
        }
        if (Integer.signum(predictedHome - predictedAway) == Integer.signum(actualHome - actualAway)) {
            return CORRECT_OUTCOME_POINTS;
        }
        return 0;
    }
}