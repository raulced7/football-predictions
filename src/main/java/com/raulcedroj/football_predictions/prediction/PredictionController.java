package com.raulcedroj.football_predictions.prediction;

import com.raulcedroj.football_predictions.prediction.dto.PredictionRequest;
import com.raulcedroj.football_predictions.prediction.dto.PredictionResponse;
import com.raulcedroj.football_predictions.prediction.dto.RoundPredictionItem;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PutMapping("/api/fixtures/{fixtureId}/prediction")
    public PredictionResponse save(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable Long fixtureId,
                                   @Valid @RequestBody PredictionRequest request) {
        return predictionService.save(Long.valueOf(jwt.getSubject()), fixtureId, request);
    }

    @GetMapping("/api/rounds/{roundId}/predictions/me")
    public List<RoundPredictionItem> findMyRound(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable Long roundId) {
        return predictionService.findMyRound(Long.valueOf(jwt.getSubject()), roundId);
    }
}