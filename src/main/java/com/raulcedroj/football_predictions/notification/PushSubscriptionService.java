package com.raulcedroj.football_predictions.notification;

import com.raulcedroj.football_predictions.notification.dto.PushSubscriptionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class PushSubscriptionService {

    /**
     * El servidor hace peticiones a la URL que manda el navegador: solo se aceptan los servicios de push
     * conocidos, para que nadie pueda usarlo para llamar a otras direcciones (SSRF).
     */
    private static final List<String> PUSH_SERVICE_DOMAINS = List.of(
            "fcm.googleapis.com",          // Chrome, Edge en Android, Samsung Internet…
            "push.apple.com",              // Safari e iPhone
            "push.services.mozilla.com",   // Firefox
            "notify.windows.com");         // Edge en Windows

    private final PushSubscriptionRepository repository;
    private final Clock clock;

    public PushSubscriptionService(PushSubscriptionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** @return la suscripción, y si es nueva (para mandarle la notificación de bienvenida) */
    @Transactional
    public Subscribed subscribe(Long userId, PushSubscriptionRequest request) {
        validate(request);
        String p256dh = request.keys().p256dh();
        String auth = request.keys().auth();

        Optional<PushSubscriptionEntity> existing = repository.findByEndpoint(request.endpoint());
        if (existing.isPresent()) {
            existing.get().reassign(userId, p256dh, auth);
            return new Subscribed(existing.get(), false);
        }
        PushSubscriptionEntity created = repository.save(
                new PushSubscriptionEntity(userId, request.endpoint(), p256dh, auth, Instant.now(clock)));
        return new Subscribed(created, true);
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        repository.deleteByEndpointAndUserId(endpoint, userId);
    }

    private void validate(PushSubscriptionRequest request) {
        URI endpoint;
        try {
            endpoint = URI.create(request.endpoint());
        } catch (IllegalArgumentException e) {
            throw new InvalidPushSubscriptionException("La suscripción no es válida.");
        }
        String host = endpoint.getHost();
        if (!"https".equals(endpoint.getScheme()) || host == null || !isPushService(host)) {
            throw new InvalidPushSubscriptionException("Este navegador usa un servicio de avisos que no admitimos.");
        }
        if (decodedLength(request.keys().p256dh()) != 65 || decodedLength(request.keys().auth()) != 16) {
            throw new InvalidPushSubscriptionException("La suscripción no es válida.");
        }
    }

    private static boolean isPushService(String host) {
        String lowerHost = host.toLowerCase();
        return PUSH_SERVICE_DOMAINS.stream()
                .anyMatch(domain -> lowerHost.equals(domain) || lowerHost.endsWith("." + domain));
    }

    private static int decodedLength(String base64Url) {
        try {
            return Base64.getUrlDecoder().decode(base64Url).length;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    public record Subscribed(PushSubscriptionEntity subscription, boolean created) {
    }
}
