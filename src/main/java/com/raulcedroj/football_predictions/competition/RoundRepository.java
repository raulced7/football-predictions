package com.raulcedroj.football_predictions.competition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RoundRepository extends JpaRepository<RoundEntity, Long> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE round r
            SET starts_at = computed.starts_at
            FROM (
                SELECT f.round_id, MIN(f.kickoff_at) AS starts_at
                FROM fixture f
                JOIN (
                    SELECT round_id,
                           percentile_disc(0.5) WITHIN GROUP (ORDER BY kickoff_at) AS median_kickoff
                    FROM fixture
                    WHERE status NOT IN ('POSTPONED', 'CANCELLED')
                    GROUP BY round_id
                ) m ON m.round_id = f.round_id
                WHERE f.status NOT IN ('POSTPONED', 'CANCELLED')
                  AND f.kickoff_at >= m.median_kickoff - INTERVAL '4 days'
                GROUP BY f.round_id
            ) computed
            WHERE r.id = computed.round_id
              AND r.season_id = :seasonId
              AND (r.starts_at IS NULL OR r.starts_at > now())
            """, nativeQuery = true)
    int recomputeStarts(@Param("seasonId") Long seasonId);

    List<RoundEntity> findBySeasonIdOrderBySequenceAsc(Long seasonId);
}