package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.fixture.dto.CreateFixtureRequest;
import com.raulcedroj.football_predictions.fixture.dto.FixtureResponse;
import com.raulcedroj.football_predictions.fixture.dto.ScheduleFixtureRequest;
import com.raulcedroj.football_predictions.fixture.dto.SetResultRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/fixtures")
public class AdminFixtureController {

    private final FixtureService fixtureService;

    public AdminFixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FixtureResponse create(@Valid @RequestBody CreateFixtureRequest request) {
        return fixtureService.create(request);
    }

    @PutMapping("/{fixtureId}/result")
    public FixtureResponse setResult(@PathVariable Long fixtureId,
                                     @Valid @RequestBody SetResultRequest request) {
        return fixtureService.setResult(fixtureId, request);
    }

    @PostMapping("/{fixtureId}/postpone")
    public FixtureResponse postpone(@PathVariable Long fixtureId) {
        return fixtureService.postpone(fixtureId);
    }

    @PutMapping("/{fixtureId}/schedule")
    public FixtureResponse reschedule(@PathVariable Long fixtureId,
                                      @Valid @RequestBody ScheduleFixtureRequest request) {
        return fixtureService.reschedule(fixtureId, request);
    }

    @PostMapping("/{fixtureId}/suspend")
    public FixtureResponse suspend(@PathVariable Long fixtureId) {
        return fixtureService.suspend(fixtureId);
    }

    @PostMapping("/{fixtureId}/cancel")
    public FixtureResponse cancel(@PathVariable Long fixtureId) {
        return fixtureService.cancel(fixtureId);
    }
}