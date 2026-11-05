package com.raulcedroj.football_predictions.sync;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSyncController {

    private final CalendarSyncService calendarSyncService;

    public AdminSyncController(CalendarSyncService calendarSyncService) {
        this.calendarSyncService = calendarSyncService;
    }

    @PostMapping("/api/admin/sync/seasons/{seasonId}")
    public SyncReport syncSeason(@PathVariable Long seasonId) {
        return calendarSyncService.syncSeason(seasonId);
    }
}