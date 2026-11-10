package com.raulcedroj.football_predictions.email;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.List;

/**
 * Envía los correos con la API HTTP de Resend. Railway bloquea el SMTP saliente en el plan Hobby,
 * así que no se puede usar el SMTP de toda la vida.
 */
class ResendEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String from;

    ResendEmailSender(RestClient.Builder builder, MailProperties properties) {
        this.from = properties.from();
        this.restClient = builder
                .baseUrl(properties.resendBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.resendApiKey())
                .build();
    }

    @Override
    public void send(EmailMessage message) {
        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ResendEmail(from, List.of(message.to()), message.subject(), message.text(), message.html()))
                .retrieve()
                .toBodilessEntity();
    }

    record ResendEmail(String from, List<String> to, String subject, String text, String html) {
    }
}