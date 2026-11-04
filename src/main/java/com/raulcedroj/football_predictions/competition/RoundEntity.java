package com.raulcedroj.football_predictions.competition;

import jakarta.persistence.*;

@Entity
@Table(name = "round")
public class RoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id")
    private SeasonEntity season;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false, length = 50)
    private String name;

    protected RoundEntity() {
    }

    public Long getId() { return id; }
    public SeasonEntity getSeason() { return season; }
    public int getSequence() { return sequence; }
    public String getName() { return name; }
}