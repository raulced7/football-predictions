package com.raulcedroj.football_predictions.team;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "team")
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 50)
    private String externalId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(length = 5)
    private String tla;

    @Column(name = "crest_url")
    private String crestUrl;

    protected TeamEntity() {
    }

    /** Equipo creado a mano por un administrador: solo tiene nombre. */
    public TeamEntity(String name) {
        this.name = name;
    }

    /** Equipo que viene del proveedor de datos. */
    public TeamEntity(String externalId, String name, String shortName, String tla, String crestUrl) {
        this.externalId = externalId;
        this.name = name;
        this.shortName = shortName;
        this.tla = tla;
        this.crestUrl = crestUrl;
    }

    /**
     * Vincula el equipo al proveedor (si aún no lo estaba) y actualiza sus datos.
     * Devuelve {@code true} si ha cambiado algo.
     */
    public boolean updateFromProvider(String externalId, String name, String shortName, String tla, String crestUrl) {
        boolean changed = !externalId.equals(this.externalId)
                || !name.equals(this.name)
                || !Objects.equals(shortName, this.shortName)
                || !Objects.equals(tla, this.tla)
                || !Objects.equals(crestUrl, this.crestUrl);
        this.externalId = externalId;
        this.name = name;
        this.shortName = shortName;
        this.tla = tla;
        this.crestUrl = crestUrl;
        return changed;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getTla() { return tla; }
    public String getCrestUrl() { return crestUrl; }
}