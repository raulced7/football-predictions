package com.raulcedroj.football_predictions.avatar;

import com.raulcedroj.football_predictions.common.exception.NotFoundException;
import com.raulcedroj.football_predictions.user.UserEntity;
import com.raulcedroj.football_predictions.user.UserRepository;
import com.raulcedroj.football_predictions.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;

@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);

    private final UserRepository userRepository;
    private final AvatarImageProcessor imageProcessor;
    private final AvatarStorage storage;

    public AvatarService(UserRepository userRepository, AvatarImageProcessor imageProcessor, AvatarStorage storage) {
        this.userRepository = userRepository;
        this.imageProcessor = imageProcessor;
        this.storage = storage;
    }

    /**
     * Sube la foto nueva y, cuando la transacción se confirma, borra la anterior. Si la transacción
     * falla, se borra la nueva, para no dejar archivos huérfanos en el almacenamiento.
     */
    @Transactional
    public UserResponse upload(Long userId, byte[] image) {
        byte[] jpeg = imageProcessor.toSquareJpeg(image);
        UserEntity user = getUser(userId);

        // Nombre único en cada subida: así la URL cambia y nadie ve la foto anterior desde la caché
        String newUrl = storage.store(userId + "/" + UUID.randomUUID() + ".jpg", jpeg);
        String oldUrl = user.getAvatarUrl();
        user.changeAvatarUrl(newUrl);

        afterTransaction(oldUrl, newUrl);
        return UserResponse.from(user);
    }

    @Transactional
    public void remove(Long userId) {
        UserEntity user = getUser(userId);
        String oldUrl = user.getAvatarUrl();
        user.changeAvatarUrl(null);
        deleteAfterCommit(oldUrl);
    }

    /** Borra la foto del almacenamiento solo si la transacción en curso se confirma. */
    public void deleteAfterCommit(String url) {
        afterTransaction(url, null);
    }

    private void afterTransaction(String deleteOnCommit, String deleteOnRollback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                deleteQuietly(status == STATUS_COMMITTED ? deleteOnCommit : deleteOnRollback);
            }
        });
    }

    /** Si falla el borrado, queda un archivo huérfano: no merece la pena que falle la petición por eso. */
    private void deleteQuietly(String url) {
        if (url == null) {
            return;
        }
        try {
            storage.delete(url);
        } catch (RuntimeException e) {
            log.warn("Could not delete avatar {}", url, e);
        }
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("No existe el usuario " + userId));
    }
}
