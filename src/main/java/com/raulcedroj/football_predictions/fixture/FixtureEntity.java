package com.raulcedroj.football_predictions.fixture;

import com.raulcedroj.football_predictions.common.exception.BusinessRuleException;
import com.raulcedroj.football_predictions.competition.RoundEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;

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

    public FixtureEntity(RoundEntity round, String homeTeam, String awayTeam,
                         Instant kickoffAt, boolean kickoffConfirmed, String externalId) {
        this(round, homeTeam, awayTeam, kickoffAt, kickoffConfirmed);
        this.externalId = externalId;
    }

    public void markLive() {
        if (status != FixtureStatus.SCHEDULED && status != FixtureStatus.SUSPENDED) {
            throw new BusinessRuleException("No se puede poner en juego un partido " + status.name().toLowerCase());
        }
        this.status = FixtureStatus.LIVE;
    }

    public boolean canBeRescheduled() {
        return status == FixtureStatus.SCHEDULED || status == FixtureStatus.POSTPONED
                || status == FixtureStatus.SUSPENDED;
    }

    public boolean hasResult(int homeGoals, int awayGoals) {
        return Objects.equals(this.homeGoals, homeGoals) && Objects.equals(this.awayGoals, awayGoals);
    }

    public void finish(int homeGoals, int awayGoals) {
        if (status == FixtureStatus.CANCELLED || status == FixtureStatus.POSTPONED) {
            throw new BusinessRuleException(
                    "No se puede poner resultado a un partido " + status.name().toLowerCase());
        }
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.status = FixtureStatus.FINISHED;
    }

    public void postpone() {
        if (status != FixtureStatus.SCHEDULED) {
            throw new BusinessRuleException("Solo se puede aplazar un partido programado");
        }
        this.status = FixtureStatus.POSTPONED;
    }

    public void reschedule(Instant newKickoff, boolean confirmed, RoundEntity newScoringRound) {
        if (status != FixtureStatus.SCHEDULED && status != FixtureStatus.POSTPONED
                && status != FixtureStatus.SUSPENDED) {
            throw new BusinessRuleException("No se puede cambiar la fecha de un partido " + status.name().toLowerCase());
        }
        this.kickoffAt = newKickoff;
        this.kickoffConfirmed = confirmed;
        this.scoringRound = newScoringRound;
        if (status == FixtureStatus.POSTPONED) {
            this.status = FixtureStatus.SCHEDULED;
        }
    }

    public void suspend(Instant now) {
        boolean started = status == FixtureStatus.LIVE
                || (status == FixtureStatus.SCHEDULED && !now.isBefore(kickoffAt));
        if (!started) {
            throw new BusinessRuleException("Solo se puede suspender un partido que ya ha empezado; si no, aplázalo");
        }
        this.status = FixtureStatus.SUSPENDED;
    }

    public void cancel() {
        if (status == FixtureStatus.CANCELLED) {
            throw new BusinessRuleException("El partido ya está cancelado");
        }
        this.status = FixtureStatus.CANCELLED;
        this.homeGoals = null;
        this.awayGoals = null;
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