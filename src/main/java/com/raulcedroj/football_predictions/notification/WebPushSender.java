package com.raulcedroj.football_predictions.notification;

import com.raulcedroj.football_predictions.notification.webpush.VapidKeys;
import com.raulcedroj.football_predictions.notification.webpush.WebPushEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Envía una notificación a un dispositivo: cifra el mensaje con las claves de la suscripción y lo entrega
 * al servicio de push del navegador (FCM en Android, Apple en iPhone…), que lo hace llegar al móvil.
 */
@Component
public class WebPushSender {

    public enum Result { SENT, GONE, FAILED }

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    private final VapidKeys vapidKeys;
    private final RestClient restClient;
    private final Clock clock;

    public WebPushSender(PushProperties properties, Clock clock) {
        this.clock = clock;
        if (properties.isConfigured()) {
            this.vapidKeys = VapidKeys.parse(properties.publicKey(), properties.privateKey(), properties.subject());
        } else {
            log.warn("VAPID keys are not set: push notifications are disabled");
            this.vapidKeys = null;
        }
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean isEnabled() {
        return vapidKeys != null;
    }

    /** Clave pública VAPID: el navegador la necesita para suscribirse. */
    public String publicKey() {
        return isEnabled() ? vapidKeys.publicKey() : null;
    }

    /**
     * @param timeToLive cuánto tiempo guarda el mensaje el servicio de push si el móvil está apagado
     * @return {@code GONE} si la suscripción ya no existe (el usuario quitó el permiso o borró los datos):
     *         hay que olvidarla
     */
    public Result send(PushTarget target, PushMessage message, Duration timeToLive) {
        if (!isEnabled()) {
            return Result.FAILED;
        }
        try {
            URI endpoint = URI.create(target.endpoint());
            Base64.Decoder base64 = Base64.getUrlDecoder();
            byte[] body = WebPushEncryption.encrypt(
                    message.toJson().getBytes(StandardCharsets.UTF_8), base64.decode(target.p256dh()), base64.decode(target.auth()));

            restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, vapidKeys.authorizationHeader(endpoint, Instant.now(clock)))
                    .header("TTL", String.valueOf(timeToLive.toSeconds()))
                    .header("Urgency", "normal")
                    .header(HttpHeaders.CONTENT_ENCODING, "aes128gcm")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return Result.SENT;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND) || e.getStatusCode().isSameCodeAs(HttpStatus.GONE)) {
                return Result.GONE;
            }
            log.warn("Push service rejected a notification: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Result.FAILED;
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("Could not send a push notification: {}", e.getMessage());
            return Result.FAILED;
        }
    }
}
