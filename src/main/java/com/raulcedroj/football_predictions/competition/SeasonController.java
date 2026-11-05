package com.raulcedroj.football_predictions.competition;

import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.competition.dto.RoundResponse;
import com.raulcedroj.football_predictions.competition.dto.SeasonResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class SeasonController {

    private final SeasonRepository seasonRepository;
    private final RoundRepository roundRepository;

    public SeasonController(SeasonRepository seasonRepository, RoundRepository roundRepository) {
        this.seasonRepository = seasonRepository;
        this.roundRepository = roundRepository;
    }

    @GetMapping("/api/seasons")
    @Transactional(readOnly = true)
    public List<SeasonResponse> findOpenSeasons() {
        return seasonRepository.findByStatusNotOrderByStartDateDesc(SeasonStatus.FINISHED).stream()
                .map(SeasonResponse::from)
                .toList();
    }

    @GetMapping("/api/seasons/{seasonId}/rounds")
    @Transactional(readOnly = true)
    public List<RoundResponse> findRounds(@PathVariable Long seasonId) {
        if (!seasonRepository.existsById(seasonId)) {
            throw new NotFoundException("No existe la temporada " + seasonId);
        }
        return roundRepository.findBySeasonIdOrderBySequenceAsc(seasonId).stream()
                .map(RoundResponse::from)
                .toList();
    }
}