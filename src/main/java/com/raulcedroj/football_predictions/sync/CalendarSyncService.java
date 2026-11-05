package com.raulcedroj.football_predictions.sync;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.competition.SeasonEntity;
import com.raulcedroj.football_predictions.competition.SeasonRepository;
import com.raulcedroj.football_predictions.sync.provider.ExternalMatch;
import com.raulcedroj.football_predictions.sync.provider.FootballDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CalendarSyncService {

    private static final Logger log = LoggerFactory.getLogger(CalendarSyncService.class);

    private final SeasonRepository seasonRepository;
    private final FootballDataProvider provider;
    private final FixtureSyncWriter writer;
    private final ReentrantLock lock = new ReentrantLock();

    public CalendarSyncService(SeasonRepository seasonRepository, FootballDataProvider provider,
                               FixtureSyncWriter writer) {
        this.seasonRepository = seasonRepository;
        this.provider = provider;
        this.writer = writer;
    }

    public SyncReport syncSeason(Long seasonId) {
        if (!lock.tryLock()) {
            throw new BusinessRuleException("Ya hay una sincronización en curso; inténtalo en unos minutos");
        }
        try {
            SeasonEntity season = seasonRepository.findWithCompetitionById(seasonId)
                    .orElseThrow(() -> new NotFoundException("No existe la temporada " + seasonId));

            String competitionId = season.getCompetition().getExternalId();
            if (competitionId == null || season.getExternalId() == null) {
                throw new BusinessRuleException("La temporada no está vinculada al proveedor de datos");
            }

            List<ExternalMatch> matches = provider.fetchMatches(competitionId, season.getExternalId());
            SyncReport report = writer.apply(seasonId, matches);

            log.info("Season {} synced: {}", seasonId, report);
            return report;
        } finally {
            lock.unlock();
        }
    }


}