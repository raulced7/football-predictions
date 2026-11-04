package com.raulcedroj.football_predictions.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FixtureRepository extends JpaRepository<FixtureEntity, Long> {

    List<FixtureEntity> findByScoringRoundIdOrderByKickoffAtAsc(Long scoringRoundId);
}