package com.raulcedroj.football_predictions.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "app.push", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
class RoundNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoundNotificationScheduler.class);

    private final RoundNotificationService service;
    private final PushNotificationService pushService;
    private final Clock clock;

    RoundNotificationScheduler(RoundNotificationService service, PushNotificationService pushService, Clock clock) {
        this.service = service;
        this.pushService = pushService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.push.check-interval:PT10M}", initialDelayString = "PT2M")
    void notifyRounds() {
        if (!pushService.isEnabled()) {
            return;
        }
        Instant now = Instant.now(clock);
        for (Long roundId : service.roundsToRemind(now)) {
            try {
                service.remind(roundId, now);
            } catch (RuntimeException e) {
                log.error("Round {} reminder failed", roundId, e);
            }
        }
        for (Long roundId : service.roundsToSummarize(now)) {
            try {
                service.summarize(roundId);
            } catch (RuntimeException e) {
                log.error("Round {} summary failed", roundId, e);
            }
        }
    }
}
