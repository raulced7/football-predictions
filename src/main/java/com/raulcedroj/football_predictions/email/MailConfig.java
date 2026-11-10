package com.raulcedroj.football_predictions.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    EmailSender emailSender(MailProperties properties) {
        if (!StringUtils.hasText(properties.resendApiKey())) {
            log.warn("RESEND_API_KEY is not set: emails will only be written to the log");
            return new LoggingEmailSender();
        }
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return new ResendEmailSender(RestClient.builder().requestFactory(requestFactory), properties);
    }
}