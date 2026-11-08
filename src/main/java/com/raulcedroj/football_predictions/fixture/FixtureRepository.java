package com.raulcedroj.football_predictions.fixture;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FixtureRepository extends JpaRepository<FixtureEntity, Long> {

    @EntityGraph(attributePaths = {"round", "scoringRound", "homeTeam", "awayTeam"})
    List<FixtureEntity> findByScoringRoundIdOrderByKickoffAtAsc(Long scoringRoundId);

    @Query(value = """
            SELECT r.id
            FROM round r
            WHERE r.season_id = :seasonId
              AND r.sequence >= :officialSequence
              AND r.starts_at IS NOT NULL
              AND r.starts_at <= :kickoffAt
            ORDER BY r.sequence DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findScoringRoundIdFor(@Param("seasonId") Long seasonId,
                                         @Param("officialSequence") int officialSequence,
                                         @Param("kickoffAt") Instant kickoffAt);

    @EntityGraph(attributePaths = "round")
    List<FixtureEntity> findByExternalIdIn(Collection<String> externalIds);

    long countByRoundSeasonId(Long seasonId);

    long countByRoundSeasonIdAndStatusNotIn(Long seasonId, Collection<FixtureStatus> statuses);

    boolean existsByRoundSeasonIdAndStatusIn(Long seasonId, Collection<FixtureStatus> statuses);

    boolean existsByRoundSeasonIdAndStatusInAndKickoffAtBetweenAndExternalIdIsNotNull(
            Long seasonId, Collection<FixtureStatus> statuses, Instant from, Instant to);
}