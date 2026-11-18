package com.raulcedroj.football_predictions.avatar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Para desarrollo: guarda las fotos en una carpeta y el backend las sirve en {@code /api/avatars/**}. */
class LocalAvatarStorage implements AvatarStorage {

    static final String URL_PREFIX = "/api/avatars/";

    private final Path directory;

    LocalAvatarStorage(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    Path directory() {
        return directory;
    }

    @Override
    public String store(String key, byte[] jpeg) {
        try {
            Path file = resolve(key);
            Files.createDirectories(file.getParent());
            Files.write(file, jpeg);
            return URL_PREFIX + key;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(URL_PREFIX)) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(publicUrl.substring(URL_PREFIX.length())));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Evita que una clave con "../" se salga de la carpeta. */
    private Path resolve(String key) {
        Path file = directory.resolve(key).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid avatar key: " + key);
        }
        return file;
    }
}
