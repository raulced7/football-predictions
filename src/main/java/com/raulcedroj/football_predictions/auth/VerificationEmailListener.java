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
 * Envía el correo con el código cuando la transacción ya se ha confirmado: si el registro fallara
 * después de enviarlo, el usuario recibiría un código para una cuenta que no existe. Y si el envío
 * falla, la cuenta queda creada y el usuario puede pedir otro código.
 */
@Component
public class VerificationEmailListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailListener.class);

    private final EmailSender emailSender;

    VerificationEmailListener(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @TransactionalEventListener
    public void onCodeIssued(VerificationCodeIssuedEvent event) {
        try {
            emailSender.send(buildMessage(event));
        } catch (RuntimeException e) {
            log.error("Could not send the verification email to user {}", event.displayName(), e);
        }
    }

    private static EmailMessage buildMessage(VerificationCodeIssuedEvent event) {
        long minutes = EmailCodeService.TIME_TO_LIVE.toMinutes();
        String subject = "%s es tu código de Golify".formatted(event.code());
        String text = """
                Hola, %s:

                Tu código para verificar tu email en Golify es: %s

                Caduca en %d minutos. Si no has creado una cuenta en Golify, ignora este correo.
                """.formatted(event.displayName(), event.code(), minutes);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#0A0E1A">
                  <p>Hola, %s:</p>
                  <p>Tu código para verificar tu email en <strong>Golify</strong> es:</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:8px;margin:24px 0">%s</p>
                  <p style="color:#6F7A96;font-size:14px">Caduca en %d minutos. Si no has creado una cuenta en Golify, ignora este correo.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(event.displayName()), event.code(), minutes);
        return new EmailMessage(event.email(), subject, text, html);
    }
}