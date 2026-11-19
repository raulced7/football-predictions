package com.raulcedroj.football_predictions.notification;

import com.raulcedroj.football_predictions.notification.dto.PushConfigResponse;
import com.raulcedroj.football_predictions.notification.dto.PushSubscriptionRequest;
import com.raulcedroj.football_predictions.notification.dto.PushUnsubscribeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private static final PushMessage WELCOME = new PushMessage(
            "¡Avisos activados!",
            "Te avisaremos antes de cada jornada si te falta algún pronóstico, y al acabar con tus puntos.",
            "/",
            "welcome");

    private final PushSubscriptionService subscriptionService;
    private final PushNotificationService notificationService;
    private final WebPushSender sender;

    public PushController(PushSubscriptionService subscriptionService, PushNotificationService notificationService,
                          WebPushSender sender) {
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
        this.sender = sender;
    }

    @GetMapping("/config")
    public PushConfigResponse config() {
        return new PushConfigResponse(sender.isEnabled(), sender.publicKey());
    }

    /** Idempotente: el front lo llama cada vez que arranca para mantener la suscripción al día. */
    @PutMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PushSubscriptionRequest request) {
        PushSubscriptionService.Subscribed result =
                subscriptionService.subscribe(Long.valueOf(jwt.getSubject()), request);
        // Solo la primera vez: confirma al usuario que los avisos le llegan de verdad
        if (result.created() && notificationService.isEnabled()) {
            notificationService.send(result.subscription(), WELCOME, Duration.ofHours(1));
        }
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PushUnsubscribeRequest request) {
        subscriptionService.unsubscribe(Long.valueOf(jwt.getSubject()), request.endpoint());
    }
}
