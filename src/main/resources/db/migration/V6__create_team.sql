CREATE TABLE team (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_id VARCHAR(50)  UNIQUE,
    name        VARCHAR(100) NOT NULL,
    short_name  VARCHAR(50),
    tla         VARCHAR(5),
    crest_url   VARCHAR(255)
);
CREATE UNIQUE INDEX uq_team_name ON team (lower(name));

-- Un equipo por cada nombre distinto que ya había en los partidos
INSERT INTO team (name)
SELECT DISTINCT ON (lower(team_name)) team_name
FROM (SELECT home_team AS team_name FROM fixture
      UNION ALL
      SELECT away_team FROM fixture) AS names
ORDER BY lower(team_name), team_name;

ALTER TABLE fixture
    ADD COLUMN home_team_id BIGINT REFERENCES team (id),
    ADD COLUMN away_team_id BIGINT REFERENCES team (id);

UPDATE fixture f
SET home_team_id = (SELECT t.id FROM team t WHERE lower(t.name) = lower(f.home_team)),
    away_team_id = (SELECT t.id FROM team t WHERE lower(t.name) = lower(f.away_team));

ALTER TABLE fixture
    ALTER COLUMN home_team_id SET NOT NULL,
    ALTER COLUMN away_team_id SET NOT NULL,
    ADD CONSTRAINT chk_fixture_different_teams CHECK (home_team_id <> away_team_id),
    DROP COLUMN home_team,
    DROP COLUMN away_team;

CREATE INDEX idx_fixture_home_team ON fixture (home_team_id);
CREATE INDEX idx_fixture_away_team ON fixture (away_team_id);