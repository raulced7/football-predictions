package com.raulcedroj.football_predictions.prediction;

import com.raulcedroj.football_predictions.fixture.FixtureEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "prediction")
public class PredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixture_id")
    private FixtureEntity fixture;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "home_goals", nullable = false)
    private Integer homeGoals;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "away_goals", nullable = false)
    private Integer awayGoals;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer points;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PredictionEntity() {
    }

    public void assignPoints(int points) {
        this.points = points;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public FixtureEntity getFixture() { return fixture; }
    public Integer getHomeGoals() { return homeGoals; }
    public Integer getAwayGoals() { return awayGoals; }
    public Integer getPoints() { return points; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}