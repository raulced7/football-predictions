package com.raulcedroj.football_predictions.email;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {

    @Test
    void sendsTheEmailThroughTheResendApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MailProperties properties = new MailProperties("Golify <no-reply@golifyapp.com>", "test-key",
                "https://api.resend.com");
        ResendEmailSender sender = new ResendEmailSender(builder, properties);

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.from").value("Golify <no-reply@golifyapp.com>"))
                .andExpect(jsonPath("$.to[0]").value("raul@example.com"))
                .andExpect(jsonPath("$.subject").value("123456 es tu código de Golify"))
                .andExpect(jsonPath("$.text").value("texto"))
                .andExpect(jsonPath("$.html").value("<p>html</p>"))
                .andRespond(withSuccess("{ \"id\": \"email-1\" }", MediaType.APPLICATION_JSON));

        sender.send(new EmailMessage("raul@example.com", "123456 es tu código de Golify", "texto", "<p>html</p>"));

        server.verify();
    }
}