package com.raulcedroj.football_predictions.competition;

import jakarta.persistence.*;

@Entity
@Table(name = "competition")
public class CompetitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String country;

    @Column(name = "external_id", length = 50)
    private String externalId;

    protected CompetitionEntity() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getExternalId() { return externalId; }
}