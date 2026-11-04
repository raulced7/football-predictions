package com.raulcedroj.football_predictions.competition;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<RoundEntity, Long> {
}