package com.raulcedroj.football_predictions.auth;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.TestcontainersConfiguration;
import com.raulcedroj.football_predictions.email.EmailMessage;
import com.raulcedroj.football_predictions.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.sync.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmailVerificationIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void registeredUserMustVerifyTheEmailBeforeLoggingIn() throws Exception {
        register("verify-ok@example.com", "verify-ok").andExpect(status().isCreated());

        EmailMessage email = lastEmail();
        assertThat(email.to()).isEqualTo("verify-ok@example.com");
        String code = codeFrom(email);

        login("verify-ok@example.com")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        String token = JsonPath.read(verifyEmail("verify-ok@example.com", code)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("verify-ok"));
        login("verify-ok@example.com").andExpect(status().isOk());

        // Un código ya usado no sirve otra vez
        verifyEmail("verify-ok@example.com", code).andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordIsReportedBeforeTheMissingVerification() throws Exception {
        register("verify-pass@example.com", "verify-pass").andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "verify-pass@example.com", "password": "wrong-password" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blocksTheCodeAfterTooManyWrongAttempts() throws Exception {
        register("verify-attempts@example.com", "verify-attempts").andExpect(status().isCreated());
        String code = codeFrom(lastEmail());
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        for (int attempt = 1; attempt < 5; attempt++) {
            verifyEmail("verify-attempts@example.com", wrongCode)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("El código no es correcto."));
        }
        verifyEmail("verify-attempts@example.com", wrongCode)
                .andExpect(jsonPath("$.detail").value("Demasiados intentos. Pide un código nuevo."));

        // Ni siquiera el código bueno funciona ya: hay que pedir otro
        verifyEmail("verify-attempts@example.com", code).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsExpiredCodes() throws Exception {
        register("verify-expired@example.com", "verify-expired").andExpect(status().isCreated());
        String code = codeFrom(lastEmail());
        jdbcTemplate.update("""
                UPDATE email_code SET expires_at = now() - interval '1 minute'
                WHERE user_id = (SELECT id FROM app_user WHERE email = ?)
                """, "verify-expired@example.com");

        verifyEmail("verify-expired@example.com", code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El código ha caducado. Pide uno nuevo."));
    }

    @Test
    void resendsANewCodeOnlyAfterTheCooldown() throws Exception {
        register("verify-resend@example.com", "verify-resend").andExpect(status().isCreated());
        String firstCode = codeFrom(lastEmail());

        // Demasiado pronto: responde igual, pero no envía nada
        resend("verify-resend@example.com").andExpect(status().isAccepted());
        verify(emailSender, times(1)).send(any());

        jdbcTemplate.update("""
                UPDATE email_code SET created_at = now() - interval '2 minutes'
                WHERE user_id = (SELECT id FROM app_user WHERE email = ?)
                """, "verify-resend@example.com");
        resend("verify-resend@example.com").andExpect(status().isAccepted());
        verify(emailSender, times(2)).send(any());
        String secondCode = codeFrom(lastEmail());

        // El código nuevo sustituye al anterior
        if (!secondCode.equals(firstCode)) {
            verifyEmail("verify-resend@example.com", firstCode).andExpect(status().isBadRequest());
        }
        verifyEmail("verify-resend@example.com", secondCode).andExpect(status().isOk());
    }

    @Test
    void doesNotRevealWhetherAnEmailIsRegistered() throws Exception {
        resend("nobody@example.com").andExpect(status().isAccepted());
        verifyEmail("nobody@example.com", "123456").andExpect(status().isBadRequest());
        verifyNoInteractions(emailSender);
    }

    private EmailMessage lastEmail() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }

    /** El asunto empieza por el código: "123456 es tu código de Golify". */
    private static String codeFrom(EmailMessage email) {
        String code = email.subject().substring(0, 6);
        assertThat(code).matches("\\d{6}");
        assertThat(email.text()).contains(code);
        return code;
    }

    private ResultActions register(String email, String displayName) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "displayName": "%s", "password": "%s" }
                        """.formatted(email, displayName, PASSWORD)));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, PASSWORD)));
    }

    private ResultActions verifyEmail(String email, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "code": "%s" }
                        """.formatted(email, code)));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s" }
                        """.formatted(email)));
    }
}