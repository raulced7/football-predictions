package com.raulcedroj.football_predictions.team;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /** Devuelve el equipo con ese nombre (sin distinguir mayúsculas) o lo crea. */
    @Transactional
    public TeamEntity findOrCreate(String name) {
        String trimmed = name.trim();
        return teamRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> teamRepository.save(new TeamEntity(trimmed)));
    }
}