-- Si ya hay nombres repetidos (por ejemplo, de las pruebas), se les añade el id para desempatarlos
UPDATE app_user u
SET display_name = left(u.display_name, 40) || ' ' || u.id
WHERE EXISTS (
    SELECT 1 FROM app_user other
    WHERE lower(other.display_name) = lower(u.display_name)
      AND other.id < u.id
);

CREATE UNIQUE INDEX uq_app_user_display_name ON app_user (lower(display_name));