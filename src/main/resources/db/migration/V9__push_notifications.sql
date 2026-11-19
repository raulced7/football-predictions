-- Un registro por dispositivo (navegador) que ha aceptado recibir avisos
CREATE TABLE push_subscription (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    endpoint   VARCHAR(1000) NOT NULL UNIQUE,
    p256dh     VARCHAR(200)  NOT NULL,
    auth       VARCHAR(50)   NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_push_subscription_user ON push_subscription (user_id);

-- Avisos de jornada ya enviados, para no repetirlos
CREATE TABLE round_notification (
    round_id BIGINT      NOT NULL REFERENCES round (id),
    type     VARCHAR(20) NOT NULL CHECK (type IN ('REMINDER', 'SUMMARY')),
    sent_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (round_id, type)
);
