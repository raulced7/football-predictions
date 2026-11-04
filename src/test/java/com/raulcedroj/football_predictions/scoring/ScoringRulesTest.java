package com.raulcedroj.football_predictions.scoring;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringRulesTest {

    private final ScoringRules rules = new ScoringRules();

    @ParameterizedTest(name = "pronóstico {0}-{1}, resultado {2}-{3} → {4} puntos")
    @CsvSource({
            // exacto
            "2, 1, 2, 1, 3",
            "0, 0, 0, 0, 3",
            // acierta el signo
            "1, 0, 3, 1, 1",
            "1, 1, 2, 2, 1",
            "0, 2, 1, 4, 1",
            // falla
            "2, 1, 1, 2, 0",
            "1, 1, 1, 0, 0",
            "0, 1, 0, 0, 0"
    })
    void calculatesPoints(int predHome, int predAway, int actualHome, int actualAway, int expected) {
        assertThat(rules.points(predHome, predAway, actualHome, actualAway)).isEqualTo(expected);
    }
}