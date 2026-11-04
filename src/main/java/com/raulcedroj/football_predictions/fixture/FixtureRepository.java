package com.raulcedroj.football_predictions.fixture;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FixtureRepository extends JpaRepository<FixtureEntity, Long> {

    @EntityGraph(attributePaths = {"round", "scoringRound"})
    List<FixtureEntity> findByScoringRoundIdOrderByKickoffAtAsc(Long scoringRoundId);
}