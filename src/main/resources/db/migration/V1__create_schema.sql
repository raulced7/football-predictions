CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));

CREATE TABLE competition (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    country     VARCHAR(50),
    external_id VARCHAR(50)  UNIQUE
);

CREATE TABLE season (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    competition_id BIGINT      NOT NULL REFERENCES competition (id),
    name           VARCHAR(20) NOT NULL,
    start_date     DATE        NOT NULL,
    end_date       DATE        NOT NULL,
    status         VARCHAR(20) NOT NULL
        CHECK (status IN ('UPCOMING', 'IN_PROGRESS', 'FINISHED')),
    external_id    VARCHAR(50) UNIQUE,
    UNIQUE (competition_id, name)
);

CREATE TABLE round (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    season_id BIGINT      NOT NULL REFERENCES season (id),
    sequence  INT         NOT NULL,
    name      VARCHAR(50) NOT NULL,
    UNIQUE (season_id, sequence)
);

CREATE TABLE fixture (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id          BIGINT       NOT NULL REFERENCES round (id),
    scoring_round_id  BIGINT       NOT NULL REFERENCES round (id),
    home_team         VARCHAR(100) NOT NULL,
    away_team         VARCHAR(100) NOT NULL,
    kickoff_at        TIMESTAMPTZ  NOT NULL,
    kickoff_confirmed BOOLEAN      NOT NULL DEFAULT false,
    status            VARCHAR(20)  NOT NULL
        CHECK (status IN ('SCHEDULED', 'LIVE', 'FINISHED', 'POSTPONED', 'SUSPENDED', 'CANCELLED')),
    home_goals        SMALLINT CHECK (home_goals >= 0),
    away_goals        SMALLINT CHECK (away_goals >= 0),
    external_id       VARCHAR(50) UNIQUE,
    CHECK (status <> 'FINISHED' OR (home_goals IS NOT NULL AND away_goals IS NOT NULL))
);
CREATE INDEX idx_fixture_round ON fixture (round_id);
CREATE INDEX idx_fixture_scoring_round ON fixture (scoring_round_id);
CREATE INDEX idx_fixture_kickoff ON fixture (kickoff_at);

CREATE TABLE prediction (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    fixture_id BIGINT      NOT NULL REFERENCES fixture (id),
    home_goals SMALLINT    NOT NULL CHECK (home_goals >= 0),
    away_goals SMALLINT    NOT NULL CHECK (away_goals >= 0),
    points     SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, fixture_id)
);
CREATE INDEX idx_prediction_fixture ON prediction (fixture_id);

CREATE TABLE friend_group (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    invite_code VARCHAR(12) NOT NULL UNIQUE,
    season_id   BIGINT      NOT NULL REFERENCES season (id),
    owner_id    BIGINT      NOT NULL REFERENCES app_user (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_friend_group_owner ON friend_group (owner_id);

CREATE TABLE friend_group_member (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    friend_group_id BIGINT      NOT NULL REFERENCES friend_group (id) ON DELETE CASCADE,
    user_id         BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (friend_group_id, user_id)
);
CREATE INDEX idx_friend_group_member_user ON friend_group_member (user_id);