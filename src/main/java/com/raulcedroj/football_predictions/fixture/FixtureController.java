package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping("/api/rounds/{roundId}/fixtures")
    public List<FixtureResponse> findByRound(@PathVariable Long roundId) {
        return fixtureService.findByRound(roundId);
    }
}