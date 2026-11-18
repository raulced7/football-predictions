package com.raulcedroj.football_predictions.avatar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.avatars")
public record AvatarProperties(String publicBaseUrl, R2 r2) {

    public record R2(String endpoint, String bucket, String accessKeyId, String secretAccessKey) {

        boolean isConfigured() {
            return StringUtils.hasText(endpoint) && StringUtils.hasText(bucket)
                    && StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey);
        }
    }

    boolean usesR2() {
        return r2 != null && r2.isConfigured();
    }
}
