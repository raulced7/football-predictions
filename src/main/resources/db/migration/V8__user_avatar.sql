-- URL pública de la foto de perfil (null = sin foto: se muestran las iniciales)
ALTER TABLE app_user ADD COLUMN avatar_url VARCHAR(255);
