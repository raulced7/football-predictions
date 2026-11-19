package com.raulcedroj.football_predictions.notification;

import com.raulcedroj.football_predictions.scoring.ScoringRules;
import com.raulcedroj.football_predictions.standings.StandingsRepository;
import com.raulcedroj.football_predictions.standings.dto.StandingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Los dos avisos de jornada:
 * <ul>
 *   <li><b>Recordatorio</b>, unas horas antes de que empiece, a quien le falten partidos por pronosticar.</li>
 *   <li><b>Resumen</b>, cuando se han jugado todos sus partidos: puntos de la jornada y posición en cada grupo.</li>
 * </ul>
 * Cada aviso se apunta en {@code round_notification} antes de enviarse, así nunca sale dos veces.
 * Una "jornada" es la jornada en la que puntúan los partidos ({@code scoring_round_id}), igual que en la app.
 */
@Service
public class RoundNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RoundNotificationService.class);

    private static final String ROUNDS_TO_REMIND_SQL = """
            SELECT r.id
            FROM round r
            WHERE r.starts_at > :now
              AND r.starts_at <= :until
              AND NOT EXISTS (SELECT 1 FROM round_notification n
                              WHERE n.round_id = r.id AND n.type = 'REMINDER')
            """;

    private static final String ROUNDS_TO_SUMMARIZE_SQL = """
            SELECT r.id
            FROM round r
            WHERE NOT EXISTS (SELECT 1 FROM round_notification n
                              WHERE n.round_id = r.id AND n.type = 'SUMMARY')
              AND EXISTS (SELECT 1 FROM fixture f
                          WHERE f.scoring_round_id = r.id AND f.status = 'FINISHED' AND f.kickoff_at >= :since)
              AND NOT EXISTS (SELECT 1 FROM fixture f
                              WHERE f.scoring_round_id = r.id AND f.status IN ('SCHEDULED', 'LIVE'))
            """;

    /** Usuarios con avisos activados y cuántos partidos aún abiertos de la jornada les faltan. */
    private static final String MISSING_PREDICTIONS_SQL = """
            SELECT s.user_id, COUNT(f.id) - COUNT(p.id) AS missing
            FROM (SELECT DISTINCT user_id FROM push_subscription) s
            CROSS JOIN fixture f
            LEFT JOIN prediction p ON p.fixture_id = f.id AND p.user_id = s.user_id
            WHERE f.scoring_round_id = :roundId
              AND f.status = 'SCHEDULED'
              AND f.kickoff_at > :now
            GROUP BY s.user_id
            HAVING COUNT(f.id) > COUNT(p.id)
            """;

    /** Usuarios con avisos activados que pronosticaron algo en la jornada, con sus puntos. */
    private static final String ROUND_POINTS_SQL = """
            SELECT p.user_id,
                   COALESCE(SUM(p.points), 0)                          AS points,
                   COUNT(p.id) FILTER (WHERE p.points = :exactPoints)  AS exact_hits
            FROM prediction p
            JOIN fixture f ON f.id = p.fixture_id
            WHERE f.scoring_round_id = :roundId
              AND p.user_id IN (SELECT user_id FROM push_subscription)
            GROUP BY p.user_id
            """;

    private static final String USER_GROUPS_SQL = """
            SELECT m.user_id, g.id AS group_id, g.name
            FROM friend_group_member m
            JOIN friend_group g ON g.id = m.friend_group_id
            WHERE g.season_id = :seasonId
              AND m.user_id IN (:userIds)
            ORDER BY g.name
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final StandingsRepository standingsRepository;
    private final PushNotificationService pushService;
    private final PushProperties properties;

    public RoundNotificationService(NamedParameterJdbcTemplate jdbc, StandingsRepository standingsRepository,
                                    PushNotificationService pushService, PushProperties properties) {
        this.jdbc = jdbc;
        this.standingsRepository = standingsRepository;
        this.pushService = pushService;
        this.properties = properties;
    }

    public List<Long> roundsToRemind(Instant now) {
        return jdbc.queryForList(ROUNDS_TO_REMIND_SQL, new MapSqlParameterSource()
                .addValue("now", Timestamp.from(now))
                .addValue("until", Timestamp.from(now.plus(properties.reminderLead()))), Long.class);
    }

    public List<Long> roundsToSummarize(Instant now) {
        return jdbc.queryForList(ROUNDS_TO_SUMMARIZE_SQL, new MapSqlParameterSource()
                .addValue("since", Timestamp.from(now.minus(properties.summaryLookback()))), Long.class);
    }

    /** @return a cuántos usuarios se ha avisado */
    public int remind(Long roundId, Instant now) {
        if (!markAsSent(roundId, "REMINDER")) {
            return 0;
        }
        RoundInfo round = roundInfo(roundId);
        // El aviso no sirve de nada una vez empezada la jornada: el servicio de push lo descarta entonces
        Duration timeToLive = round.startsAt() == null || !round.startsAt().isAfter(now)
                ? Duration.ofMinutes(5)
                : Duration.between(now, round.startsAt());

        List<Map<String, Object>> rows = jdbc.queryForList(MISSING_PREDICTIONS_SQL, new MapSqlParameterSource()
                .addValue("roundId", roundId)
                .addValue("now", Timestamp.from(now)));
        int notified = 0;
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            int missing = ((Number) row.get("missing")).intValue();
            PushMessage message = RoundMessages.reminder(roundId, round.name(), round.startsAt(), missing, now);
            if (pushService.sendToUser(userId, message, timeToLive) > 0) {
                notified++;
            }
        }
        log.info("Round {} reminder sent to {} users", roundId, notified);
        return notified;
    }

    /** @return a cuántos usuarios se ha avisado */
    public int summarize(Long roundId) {
        if (!markAsSent(roundId, "SUMMARY")) {
            return 0;
        }
        RoundInfo round = roundInfo(roundId);
        List<Map<String, Object>> rows = jdbc.queryForList(ROUND_POINTS_SQL, new MapSqlParameterSource()
                .addValue("roundId", roundId)
                .addValue("exactPoints", ScoringRules.EXACT_RESULT_POINTS));
        if (rows.isEmpty()) {
            return 0;
        }

        List<Long> userIds = rows.stream().map(row -> ((Number) row.get("user_id")).longValue()).toList();
        Map<Long, List<RoundMessages.GroupPosition>> positions = groupPositions(round.seasonId(), userIds);

        int notified = 0;
        for (Map<String, Object> row : rows) {
            Long userId = ((Number) row.get("user_id")).longValue();
            PushMessage message = RoundMessages.summary(roundId, round.name(),
                    ((Number) row.get("points")).intValue(),
                    ((Number) row.get("exact_hits")).intValue(),
                    positions.getOrDefault(userId, List.of()));
            if (pushService.sendToUser(userId, message, Duration.ofDays(1)) > 0) {
                notified++;
            }
        }
        log.info("Round {} summary sent to {} users", roundId, notified);
        return notified;
    }

    /** Posición de cada usuario en cada uno de sus grupos de la temporada (cada clasificación se calcula una vez). */
    private Map<Long, List<RoundMessages.GroupPosition>> groupPositions(Long seasonId, List<Long> userIds) {
        List<Map<String, Object>> memberships = jdbc.queryForList(USER_GROUPS_SQL, new MapSqlParameterSource()
                .addValue("seasonId", seasonId)
                .addValue("userIds", userIds));

        Map<Long, List<StandingEntry>> standingsByGroup = new HashMap<>();
        Map<Long, List<RoundMessages.GroupPosition>> result = new HashMap<>();
        for (Map<String, Object> membership : memberships) {
            Long userId = ((Number) membership.get("user_id")).longValue();
            Long groupId = ((Number) membership.get("group_id")).longValue();
            String groupName = (String) membership.get("name");
            standingsByGroup.computeIfAbsent(groupId, standingsRepository::findGeneral).stream()
                    .filter(entry -> entry.userId().equals(userId))
                    .findFirst()
                    .ifPresent(entry -> result.computeIfAbsent(userId, id -> new ArrayList<>())
                            .add(new RoundMessages.GroupPosition(groupId, groupName, entry.position())));
        }
        return result;
    }

    /** Devuelve false si ya estaba apuntado (otro proceso lo envió). */
    private boolean markAsSent(Long roundId, String type) {
        return jdbc.update("""
                INSERT INTO round_notification (round_id, type) VALUES (:roundId, :type)
                ON CONFLICT DO NOTHING
                """, new MapSqlParameterSource().addValue("roundId", roundId).addValue("type", type)) == 1;
    }

    private RoundInfo roundInfo(Long roundId) {
        return jdbc.queryForObject("SELECT name, season_id, starts_at FROM round WHERE id = :roundId",
                new MapSqlParameterSource("roundId", roundId),
                (rs, rowNum) -> new RoundInfo(
                        rs.getString("name"),
                        rs.getLong("season_id"),
                        rs.getTimestamp("starts_at") == null ? null : rs.getTimestamp("starts_at").toInstant()));
    }

    private record RoundInfo(String name, Long seasonId, Instant startsAt) {
    }
}
