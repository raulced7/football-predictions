package com.raulcedroj.football_predictions.avatar;

/** Dónde se guardan las fotos de perfil: Cloudflare R2 en producción, una carpeta local en desarrollo. */
public interface AvatarStorage {

    /** Guarda la imagen y devuelve su URL pública. */
    String store(String key, byte[] jpeg);

    /** Borra la imagen a partir de su URL pública. Si la URL no es de este almacenamiento, no hace nada. */
    void delete(String publicUrl);
}
