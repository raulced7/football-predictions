ALTER TABLE app_user ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Quien ya estaba registrado se da por verificado, para no dejar a nadie fuera
UPDATE app_user SET email_verified_at = created_at;

-- Un código activo por usuario y finalidad (verificar el email, más adelante recuperar la contraseña)
CREATE TABLE email_code (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    purpose    VARCHAR(30)  NOT NULL,
    code_hash  VARCHAR(100) NOT NULL,
    attempts   INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_email_code_user_purpose UNIQUE (user_id, purpose)
);