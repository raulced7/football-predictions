package com.raulcedroj.football_predictions.competition;

import com.raulcedroj.football_predictions.competition.dto.SeasonResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class SeasonController {

    private final SeasonRepository seasonRepository;

    public SeasonController(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    @GetMapping("/api/seasons")
    @Transactional(readOnly = true)
    public List<SeasonResponse> findOpenSeasons() {
        return seasonRepository.findByStatusNotOrderByStartDateDesc(SeasonStatus.FINISHED).stream()
                .map(SeasonResponse::from)
                .toList();
    }
}