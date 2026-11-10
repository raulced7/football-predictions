package com.raulcedroj.football_predictions.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** En local no se envía nada: el correo se escribe en el log para poder copiar el código. */
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("""
                
                ----- Email (not sent) -----
                To: {}
                Subject: {}
                
                {}
                ----------------------------""", message.to(), message.subject(), message.text());
    }
}