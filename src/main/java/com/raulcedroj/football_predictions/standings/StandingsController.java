package com.raulcedroj.football_predictions.standings;

import com.raulcedroj.football_predictions.standings.dto.GroupFixturePredictions;
import com.raulcedroj.football_predictions.standings.dto.StandingEntry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping("/standings")
    public List<StandingEntry> general(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return standingsService.general(userId(jwt), groupId);
    }

    @GetMapping("/standings/rounds/{roundId}")
    public List<StandingEntry> round(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long groupId, @PathVariable Long roundId) {
        return standingsService.round(userId(jwt), groupId, roundId);
    }

    @GetMapping("/rounds/{roundId}/predictions")
    public List<GroupFixturePredictions> roundPredictions(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable Long groupId,
                                                          @PathVariable Long roundId) {
        return standingsService.roundPredictions(userId(jwt), groupId, roundId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}