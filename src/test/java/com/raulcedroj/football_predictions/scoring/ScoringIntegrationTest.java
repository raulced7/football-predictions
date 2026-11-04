package com.raulcedroj.football_predictions.scoring;

import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScoringIntegrationTest extends IntegrationTestSupport {

    @Test
    void scoresPredictionsAndRecalculatesWhenResultIsCorrected() throws Exception {
        String admin = tokenFor("admin-scoring@example.com", true);
        String exact = tokenFor("exact@example.com", false);
        String outcome = tokenFor("outcome@example.com", false);
        String wrong = tokenFor("wrong@example.com", false);

        Integer fixtureId = createFixture(admin, roundId(4), "Athletic", "Real Sociedad",
                Instant.now().plus(Duration.ofDays(1)));

        predict(exact, fixtureId, 2, 1).andExpect(status().isOk());
        predict(outcome, fixtureId, 1, 0).andExpect(status().isOk());
        predict(wrong, fixtureId, 0, 2).andExpect(status().isOk());

        setResult(admin, fixtureId, 2, 1).andExpect(status().isOk());

        assertThat(pointsOf("exact@example.com", fixtureId)).isEqualTo(3);
        assertThat(pointsOf("outcome@example.com", fixtureId)).isEqualTo(1);
        assertThat(pointsOf("wrong@example.com", fixtureId)).isEqualTo(0);

        // El resultado se corrige: los puntos se recalculan desde cero
        setResult(admin, fixtureId, 1, 0).andExpect(status().isOk());

        assertThat(pointsOf("exact@example.com", fixtureId)).isEqualTo(1);
        assertThat(pointsOf("outcome@example.com", fixtureId)).isEqualTo(3);
        assertThat(pointsOf("wrong@example.com", fixtureId)).isEqualTo(0);
    }

    private Integer pointsOf(String email, Integer fixtureId) {
        return jdbcTemplate.queryForObject("""
                SELECT p.points FROM prediction p
                JOIN app_user u ON u.id = p.user_id
                WHERE u.email = ? AND p.fixture_id = ?
                """, Integer.class, email, fixtureId);
    }
}