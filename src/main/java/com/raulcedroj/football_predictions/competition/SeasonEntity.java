package com.raulcedroj.football_predictions.competition;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "season")
public class SeasonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id")
    private CompetitionEntity competition;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeasonStatus status;

    @Column(name = "external_id", length = 50)
    private String externalId;

    protected SeasonEntity() {
    }

    public void markInProgress() {
        if (status == SeasonStatus.UPCOMING) {
            this.status = SeasonStatus.IN_PROGRESS;
        }
    }

    public void markFinished() {
        this.status = SeasonStatus.FINISHED;
    }

    public Long getId() { return id; }
    public CompetitionEntity getCompetition() { return competition; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public SeasonStatus getStatus() { return status; }
    public String getExternalId() { return externalId; }
}