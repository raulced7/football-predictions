package com.raulcedroj.football_predictions.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import java.time.Duration;

/**
 * @param publicKey        clave pública VAPID (base64url)
 * @param privateKey       clave privada VAPID (base64url)
 * @param subject          contacto para los servicios de push ({@code mailto:...})
 * @param reminderLead     cuánto antes de que empiece la jornada se avisa
 * @param summaryLookback  solo se resumen jornadas con algún partido jugado en este margen, para no
 *                         mandar resúmenes de jornadas antiguas (por ejemplo, al activar los avisos)
 */
@ConfigurationProperties(prefix = "app.push")
public record PushProperties(
        String publicKey,
        String privateKey,
        @DefaultValue("mailto:hola@golifyapp.com") String subject,
        @DefaultValue("PT3H") Duration reminderLead,
        @DefaultValue("P3D") Duration summaryLookback) {

    public boolean isConfigured() {
        return StringUtils.hasText(publicKey) && StringUtils.hasText(privateKey);
    }
}
