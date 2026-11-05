package com.raulcedroj.football_predictions.fixture;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FixtureRepository extends JpaRepository<FixtureEntity, Long> {

    @EntityGraph(attributePaths = {"round", "scoringRound"})
    List<FixtureEntity> findByScoringRoundIdOrderByKickoffAtAsc(Long scoringRoundId);

    @Query(value = """
            SELECT r.id
            FROM round r
            JOIN fixture f ON f.round_id = r.id
                          AND f.status NOT IN ('POSTPONED', 'CANCELLED')
                          AND f.id <> :fixtureId
            WHERE r.season_id = :seasonId
              AND r.sequence >= :officialSequence
            GROUP BY r.id, r.sequence
            HAVING MIN(f.kickoff_at) <= :kickoffAt
            ORDER BY r.sequence DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findScoringRoundIdFor(@Param("fixtureId") Long fixtureId,
                                         @Param("seasonId") Long seasonId,
                                         @Param("officialSequence") int officialSequence,
                                         @Param("kickoffAt") Instant kickoffAt);
}