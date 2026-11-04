package com.raulcedroj.football_predictions.prediction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<PredictionEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO prediction (user_id, fixture_id, home_goals, away_goals)
            VALUES (:userId, :fixtureId, :homeGoals, :awayGoals)
            ON CONFLICT (user_id, fixture_id)
            DO UPDATE SET home_goals = EXCLUDED.home_goals,
                          away_goals = EXCLUDED.away_goals,
                          updated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("userId") Long userId,
                @Param("fixtureId") Long fixtureId,
                @Param("homeGoals") int homeGoals,
                @Param("awayGoals") int awayGoals);

    Optional<PredictionEntity> findByUserIdAndFixtureId(Long userId, Long fixtureId);

    List<PredictionEntity> findByUserIdAndFixtureIdIn(Long userId, Collection<Long> fixtureIds);
}