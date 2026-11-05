package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.competition.RoundEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "fixture")
public class FixtureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id")
    private RoundEntity round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scoring_round_id")
    private RoundEntity scoringRound;

    @Column(name = "home_team", nullable = false, length = 100)
    private String homeTeam;

    @Column(name = "away_team", nullable = false, length = 100)
    private String awayTeam;

    @Column(name = "kickoff_at", nullable = false)
    private Instant kickoffAt;

    @Column(name = "kickoff_confirmed", nullable = false)
    private boolean kickoffConfirmed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FixtureStatus status;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "home_goals")
    private Integer homeGoals;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "away_goals")
    private Integer awayGoals;

    @Column(name = "external_id", length = 50)
    private String externalId;

    protected FixtureEntity() {
    }

    public FixtureEntity(RoundEntity round, String homeTeam, String awayTeam,
                         Instant kickoffAt, boolean kickoffConfirmed) {
        this.round = round;
        this.scoringRound = round;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffAt = kickoffAt;
        this.kickoffConfirmed = kickoffConfirmed;
        this.status = FixtureStatus.SCHEDULED;
    }

    public void finish(int homeGoals, int awayGoals) {
        if (status == FixtureStatus.CANCELLED) {
            throw new BusinessRuleException("No se puede poner resultado a un partido cancelado");
        }
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.status = FixtureStatus.FINISHED;
    }

    public boolean isOpenForPredictions(Instant now) {
        return (status == FixtureStatus.SCHEDULED || status == FixtureStatus.POSTPONED)
                && now.isBefore(kickoffAt);
    }

    public boolean hasStarted(Instant now) {
        return switch (status) {
            case LIVE, FINISHED, SUSPENDED -> true;
            case SCHEDULED -> !now.isBefore(kickoffAt);
            case POSTPONED, CANCELLED -> false;
        };
    }

    public Long getId() { return id; }
    public RoundEntity getRound() { return round; }
    public RoundEntity getScoringRound() { return scoringRound; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public Instant getKickoffAt() { return kickoffAt; }
    public boolean isKickoffConfirmed() { return kickoffConfirmed; }
    public FixtureStatus getStatus() { return status; }
    public Integer getHomeGoals() { return homeGoals; }
    public Integer getAwayGoals() { return awayGoals; }
    public String getExternalId() { return externalId; }
}