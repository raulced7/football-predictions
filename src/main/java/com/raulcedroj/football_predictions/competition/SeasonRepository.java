package com.raulcedroj.football_predictions.competition;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<SeasonEntity, Long> {

    @EntityGraph(attributePaths = "competition")
    Optional<SeasonEntity> findWithCompetitionById(Long id);

    @EntityGraph(attributePaths = "competition")
    List<SeasonEntity> findByStatusNotOrderByStartDateDesc(SeasonStatus status);
}