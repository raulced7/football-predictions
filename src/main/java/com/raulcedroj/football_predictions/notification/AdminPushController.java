package com.raulcedroj.football_predictions.notification;

import com.raulcedroj.football_predictions.notification.dto.PushBroadcastRequest;
import com.raulcedroj.football_predictions.notification.dto.PushBroadcastResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;

@RestController
@RequestMapping("/api/admin/push")
public class AdminPushController {

    private final PushNotificationService notificationService;

    public AdminPushController(PushNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Envía una notificación a todos los usuarios que tienen los avisos activados. */
    @PostMapping("/broadcast")
    public PushBroadcastResponse broadcast(@Valid @RequestBody PushBroadcastRequest request) {
        String url = request.url() == null || request.url().isBlank() ? "/" : request.url();
        PushMessage message = new PushMessage(request.title(), request.body(), url, "broadcast");
        return notificationService.sendToAll(message, Duration.ofDays(1));
    }
}
