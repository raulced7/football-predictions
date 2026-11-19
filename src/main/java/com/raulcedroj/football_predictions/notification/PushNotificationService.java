package com.raulcedroj.football_predictions.notification;

import com.raulcedroj.football_predictions.notification.dto.PushBroadcastResponse;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
public class PushNotificationService {

    private final PushSubscriptionRepository subscriptionRepository;
    private final WebPushSender sender;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository, WebPushSender sender) {
        this.subscriptionRepository = subscriptionRepository;
        this.sender = sender;
    }

    public boolean isEnabled() {
        return sender.isEnabled();
    }

    /** A todos los dispositivos del usuario. Devuelve a cuántos ha llegado. */
    public int sendToUser(Long userId, PushMessage message, Duration timeToLive) {
        int sent = 0;
        for (PushSubscriptionEntity subscription : subscriptionRepository.findByUserId(userId)) {
            if (send(subscription, message, timeToLive)) {
                sent++;
            }
        }
        return sent;
    }

    /** A todos los dispositivos suscritos de todos los usuarios. */
    public PushBroadcastResponse sendToAll(PushMessage message, Duration timeToLive) {
        List<PushSubscriptionEntity> subscriptions = subscriptionRepository.findAll();
        int sent = 0;
        for (PushSubscriptionEntity subscription : subscriptions) {
            if (send(subscription, message, timeToLive)) {
                sent++;
            }
        }
        return new PushBroadcastResponse(subscriptions.size(), sent);
    }

    public boolean send(PushSubscriptionEntity subscription, PushMessage message, Duration timeToLive) {
        WebPushSender.Result result = sender.send(subscription.target(), message, timeToLive);
        if (result == WebPushSender.Result.GONE) {
            subscriptionRepository.delete(subscription);
        }
        return result == WebPushSender.Result.SENT;
    }
}
