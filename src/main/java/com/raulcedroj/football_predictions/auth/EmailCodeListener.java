package com.raulcedroj.football_predictions.auth;

import com.raulcedroj.football_predictions.auth.code.EmailCodeService;
import com.raulcedroj.football_predictions.email.EmailMessage;
import com.raulcedroj.football_predictions.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

/**
 * Envía el correo con el código cuando la transacción ya se ha confirmado: si algo fallara después
 * de enviarlo, el usuario recibiría un código que no sirve. Y si el envío falla, puede pedir otro.
 */
@Component
public class EmailCodeListener {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeListener.class);

    private final EmailSender emailSender;

    EmailCodeListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @TransactionalEventListener
    public void onCodeIssued(EmailCodeIssuedEvent event) {
        try {
            emailSender.send(buildMessage(event));
        } catch (RuntimeException e) {
            log.error("Could not send the {} email to user {}", event.purpose(), event.displayName(), e);
        }
    }

    private static EmailMessage buildMessage(EmailCodeIssuedEvent event) {
        long minutes = EmailCodeService.TIME_TO_LIVE.toMinutes();
        String subject = switch (event.purpose()) {
            case EMAIL_VERIFICATION -> "%s es tu código de Golify".formatted(event.code());
            case PASSWORD_RESET -> "%s es tu código para cambiar la contraseña de Golify".formatted(event.code());
        };
        String purpose = switch (event.purpose()) {
            case EMAIL_VERIFICATION -> "verificar tu email";
            case PASSWORD_RESET -> "cambiar tu contraseña";
        };
        String ignore = switch (event.purpose()) {
            case EMAIL_VERIFICATION -> "Si no has creado una cuenta en Golify, ignora este correo.";
            case PASSWORD_RESET -> "Si no lo has pedido tú, ignora este correo: tu contraseña no cambiará.";
        };

        String text = """
                Hola, %s:

                Tu código para %s en Golify es: %s

                Caduca en %d minutos. %s
                """.formatted(event.displayName(), purpose, event.code(), minutes, ignore);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#0A0E1A">
                  <p>Hola, %s:</p>
                  <p>Tu código para %s en <strong>Golify</strong> es:</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:8px;margin:24px 0">%s</p>
                  <p style="color:#6F7A96;font-size:14px">Caduca en %d minutos. %s</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(event.displayName()), purpose, event.code(), minutes, ignore);
        return new EmailMessage(event.email(), subject, text, html);
    }
}