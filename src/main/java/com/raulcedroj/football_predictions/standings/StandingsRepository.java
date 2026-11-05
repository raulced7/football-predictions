package com.raulcedroj.football_predictions.standings;

import com.raulcedroj.football_predictions.scoring.ScoringRules;
import com.raulcedroj.football_predictions.standings.dto.MemberPrediction;
import com.raulcedroj.football_predictions.standings.dto.StandingEntry;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class StandingsRepository {

    private static final String STANDINGS_SQL = """
            SELECT u.id           AS user_id,
                   u.display_name AS display_name,
                   COALESCE(SUM(p.points), 0)                           AS points,
                   COUNT(p.id) FILTER (WHERE p.points = :exactPoints)   AS exact_hits,
                   RANK() OVER (ORDER BY COALESCE(SUM(p.points), 0) DESC,
                                         COUNT(p.id) FILTER (WHERE p.points = :exactPoints) DESC) AS position
            FROM friend_group g
            JOIN friend_group_member m ON m.friend_group_id = g.id
            JOIN app_user u            ON u.id = m.user_id
            LEFT JOIN (prediction p
                       JOIN fixture f ON f.id = p.fixture_id
                       JOIN round r   ON r.id = f.scoring_round_id)
                   ON p.user_id = u.id
                  AND r.season_id = g.season_id
                  %s
            WHERE g.id = :groupId
            GROUP BY u.id, u.display_name
            ORDER BY position, u.display_name
            """;

    private static final String MEMBER_PREDICTIONS_SQL = """
            SELECT p.fixture_id, u.id AS user_id, u.display_name,
                   p.home_goals, p.away_goals, p.points
            FROM prediction p
            JOIN friend_group_member m ON m.user_id = p.user_id AND m.friend_group_id = :groupId
            JOIN app_user u            ON u.id = p.user_id
            JOIN fixture f             ON f.id = p.fixture_id
            WHERE f.scoring_round_id = :roundId
            ORDER BY u.display_name
            """;

    private static final RowMapper<StandingEntry> STANDING_MAPPER = (rs, rowNum) -> new StandingEntry(
            rs.getInt("position"),
            rs.getLong("user_id"),
            rs.getString("display_name"),
            rs.getInt("points"),
            rs.getInt("exact_hits"));

    private final NamedParameterJdbcTemplate jdbc;

    public StandingsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StandingEntry> findGeneral(Long groupId) {
        return jdbc.query(STANDINGS_SQL.formatted(""), baseParams(groupId), STANDING_MAPPER);
    }

    public List<StandingEntry> findByRound(Long groupId, Long roundId) {
        return jdbc.query(STANDINGS_SQL.formatted("AND f.scoring_round_id = :roundId"),
                baseParams(groupId).addValue("roundId", roundId), STANDING_MAPPER);
    }

    public List<MemberPredictionRow> findMemberPredictions(Long groupId, Long roundId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("roundId", roundId);

        return jdbc.query(MEMBER_PREDICTIONS_SQL, params, (rs, rowNum) -> new MemberPredictionRow(
                rs.getLong("fixture_id"),
                new MemberPrediction(
                        rs.getLong("user_id"),
                        rs.getString("display_name"),
                        rs.getInt("home_goals"),
                        rs.getInt("away_goals"),
                        rs.getObject("points", Integer.class))));
    }

    private MapSqlParameterSource baseParams(Long groupId) {
        return new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("exactPoints", ScoringRules.EXACT_RESULT_POINTS);
    }

    public record MemberPredictionRow(Long fixtureId, MemberPrediction prediction) {
    }
}