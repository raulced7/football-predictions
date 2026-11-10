package com.raulcedroj.football_predictions.auth;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.email.EmailMessage;
import com.raulcedroj.football_predictions.email.EmailSender;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private EmailSender emailSender;

    @Test
    void resetsTheForgottenPasswordWithACodeSentByEmail() throws Exception {
        tokenFor("reset-ok@example.com", false);

        requestReset("reset-ok@example.com").andExpect(status().isAccepted());
        EmailMessage email = lastEmail();
        assertThat(email.to()).isEqualTo("reset-ok@example.com");
        assertThat(email.subject()).contains("cambiar la contraseña");
        String code = codeFrom(email);

        String token = JsonPath.read(confirmReset("reset-ok@example.com", code, "new-password-1")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        assertThat(token).isNotBlank();

        login("reset-ok@example.com", "password123").andExpect(status().isUnauthorized());
        login("reset-ok@example.com", "new-password-1").andExpect(status().isOk());

        // El código solo sirve una vez
        confirmReset("reset-ok@example.com", code, "another-password").andExpect(status().isBadRequest());
    }

    @Test
    void resettingThePasswordAlsoVerifiesTheEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "reset-unverified@example.com", "displayName": "reset-unverified", "password": "password123" }
                                """))
                .andExpect(status().isCreated());

        requestReset("reset-unverified@example.com").andExpect(status().isAccepted());
        confirmReset("reset-unverified@example.com", codeFrom(lastEmail()), "new-password-1")
                .andExpect(status().isOk());

        login("reset-unverified@example.com", "new-password-1").andExpect(status().isOk());
    }

    @Test
    void rejectsAWrongCodeAndKeepsTheOldPassword() throws Exception {
        tokenFor("reset-wrong@example.com", false);
        requestReset("reset-wrong@example.com").andExpect(status().isAccepted());
        String code = codeFrom(lastEmail());
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        confirmReset("reset-wrong@example.com", wrongCode, "new-password-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El código no es correcto."));

        login("reset-wrong@example.com", "password123").andExpect(status().isOk());
    }

    @Test
    void doesNotRevealWhetherAnEmailIsRegistered() throws Exception {
        requestReset("nobody-reset@example.com").andExpect(status().isAccepted());
        verify(emailSender, never()).send(any());
    }

    @Test
    void changesThePasswordWhenTheCurrentOneIsCorrect() throws Exception {
        String token = tokenFor("change-pass@example.com", false);

        changePassword(token, "wrong-password", "new-password-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La contraseña actual no es correcta"));
        changePassword(token, "password123", "short").andExpect(status().isBadRequest());

        changePassword(token, "password123", "new-password-1").andExpect(status().isNoContent());
        login("change-pass@example.com", "new-password-1").andExpect(status().isOk());
        login("change-pass@example.com", "password123").andExpect(status().isUnauthorized());
    }

    private EmailMessage lastEmail() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }

    private static String codeFrom(EmailMessage email) {
        String code = email.subject().substring(0, 6);
        assertThat(code).matches("\\d{6}");
        return code;
    }

    private ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s" }
                        """.formatted(email)));
    }

    private ResultActions confirmReset(String email, String code, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "code": "%s", "newPassword": "%s" }
                        """.formatted(email, code, newPassword)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }
                        """.formatted(email, password)));
    }

    private ResultActions changePassword(String token, String currentPassword, String newPassword) throws Exception {
        return mockMvc.perform(put("/api/users/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "currentPassword": "%s", "newPassword": "%s" }
                        """.formatted(currentPassword, newPassword)));
    }
}