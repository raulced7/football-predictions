package com.raulcedroj.football_predictions.notification;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PushIntegrationTest extends IntegrationTestSupport {

    // Claves de suscripción válidas (del ejemplo de RFC 8291): el envío real está simulado
    private static final String P256DH =
            "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String AUTH = "BTBZMqHH6r4Tts7J_aSIgg";

    @MockitoBean
    private WebPushSender sender;

    @Autowired
    private RoundNotificationService roundNotifications;

    @BeforeEach
    void pushIsEnabled() {
        when(sender.isEnabled()).thenReturn(true);
        when(sender.publicKey()).thenReturn("test-public-key");
        when(sender.send(any(), any(), any())).thenReturn(WebPushSender.Result.SENT);
    }

    @Test
    void exposesThePublicKey() throws Exception {
        String token = tokenFor("push-config@example.com", false);

        mockMvc.perform(get("/api/push/config").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.publicKey").value("test-public-key"));
    }

    @Test
    void sendsAWelcomeOnlyTheFirstTimeADeviceSubscribes() throws Exception {
        String token = tokenFor("push-welcome@example.com", false);
        String endpoint = endpoint("welcome");

        subscribe(token, endpoint).andExpect(status().isNoContent());
        subscribe(token, endpoint).andExpect(status().isNoContent());

        verify(sender, times(1)).send(argThat(target -> target.endpoint().equals(endpoint)),
                argThat(message -> message.tag().equals("welcome")), any());
        assertThat(subscriptionsFor(endpoint)).isEqualTo(1);
    }

    @Test
    void aDeviceBelongsToTheLastUserWhoSubscribedWithIt() throws Exception {
        String alice = tokenFor("push-owner-alice@example.com", false);
        String bob = tokenFor("push-owner-bob@example.com", false);
        String endpoint = endpoint("shared-phone");

        subscribe(alice, endpoint);
        subscribe(bob, endpoint);
        assertThat(ownerOf(endpoint)).isEqualTo(userId("push-owner-bob@example.com"));

        // Alice ya no puede quitarla: ahora es de Bob
        unsubscribe(alice, endpoint).andExpect(status().isNoContent());
        assertThat(subscriptionsFor(endpoint)).isEqualTo(1);

        unsubscribe(bob, endpoint).andExpect(status().isNoContent());
        assertThat(subscriptionsFor(endpoint)).isZero();
    }

    @Test
    void onlyAcceptsKnownPushServices() throws Exception {
        String token = tokenFor("push-invalid@example.com", false);

        subscribe(token, "https://evil.example.com/fcm/send/abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid push subscription"));
        subscribe(token, "https://fcm.googleapis.com.evil.example.com/abc").andExpect(status().isBadRequest());
        subscribe(token, "http://fcm.googleapis.com/fcm/send/abc").andExpect(status().isBadRequest());
        subscribe(token, "https://web.push.apple.com/abc", "BCVx", AUTH).andExpect(status().isBadRequest());

        subscribe(token, "https://web.push.apple.com/" + System.nanoTime()).andExpect(status().isNoContent());
    }

    @Test
    void forgetsDevicesThatNoLongerExist() throws Exception {
        when(sender.send(any(), any(), any())).thenReturn(WebPushSender.Result.GONE);
        String token = tokenFor("push-gone@example.com", false);
        String endpoint = endpoint("gone");

        subscribe(token, endpoint).andExpect(status().isNoContent());

        assertThat(subscriptionsFor(endpoint)).isZero();
    }

    @Test
    void remindsOnlyUsersWithMatchesLeftToPredict() throws Exception {
        String admin = tokenFor("push-remind-admin@example.com", true);
        String alice = tokenFor("push-remind-alice@example.com", false);
        String bob = tokenFor("push-remind-bob@example.com", false);

        Long roundId = roundId(25);
        Instant start = Instant.now().plus(Duration.ofHours(2));
        Integer match1 = createFixture(admin, roundId, "Getafe", "Osasuna", start);
        Integer match2 = createFixture(admin, roundId, "Celta", "Mallorca", start.plus(Duration.ofHours(2)));
        predict(alice, match1, 1, 0);
        predict(bob, match1, 2, 2);
        predict(bob, match2, 0, 1);

        String aliceEndpoint = endpoint("remind-alice");
        String bobEndpoint = endpoint("remind-bob");
        subscribe(alice, aliceEndpoint);
        subscribe(bob, bobEndpoint);
        clearInvocations(sender);

        Instant now = Instant.now();
        assertThat(roundNotifications.roundsToRemind(now)).contains(roundId);
        roundNotifications.remind(roundId, now);

        List<PushMessage> toAlice = messagesTo(aliceEndpoint);
        assertThat(toAlice).hasSize(1);
        assertThat(toAlice.getFirst().title()).isEqualTo("Te falta 1 partido por pronosticar");
        assertThat(toAlice.getFirst().body()).startsWith("La jornada 25 empieza");
        assertThat(messagesTo(bobEndpoint)).isEmpty();

        // Nunca se repite
        clearInvocations(sender);
        assertThat(roundNotifications.remind(roundId, now)).isZero();
        assertThat(roundNotifications.roundsToRemind(now)).doesNotContain(roundId);
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    void summarizesTheRoundWithPointsAndGroupPositions() throws Exception {
        String admin = tokenFor("push-summary-admin@example.com", true);
        String alice = tokenFor("push-summary-alice@example.com", false);
        String bob = tokenFor("push-summary-bob@example.com", false);

        String group = createGroup(alice, "Resumen").andReturn().getResponse().getContentAsString();
        Integer groupId = JsonPath.read(group, "$.id");
        join(bob, JsonPath.read(group, "$.inviteCode"));

        Long roundId = roundId(26);
        Instant tomorrow = Instant.now().plus(Duration.ofDays(1));
        Integer match1 = createFixture(admin, roundId, "Sevilla", "Girona", tomorrow);
        Integer match2 = createFixture(admin, roundId, "Valencia", "Elche", tomorrow.plus(Duration.ofHours(2)));
        predict(alice, match1, 2, 1);
        predict(alice, match2, 0, 0);
        // Bob no pronostica nada en esta jornada: no recibe resumen

        String aliceEndpoint = endpoint("summary-alice");
        String bobEndpoint = endpoint("summary-bob");
        subscribe(alice, aliceEndpoint);
        subscribe(bob, bobEndpoint);

        Instant now = Instant.now();
        setResult(admin, match1, 2, 1);   // pleno: 3
        assertThat(roundNotifications.roundsToSummarize(now)).doesNotContain(roundId);
        setResult(admin, match2, 1, 1);   // empate acertado: 1
        assertThat(roundNotifications.roundsToSummarize(now)).contains(roundId);

        clearInvocations(sender);
        roundNotifications.summarize(roundId);

        List<PushMessage> toAlice = messagesTo(aliceEndpoint);
        assertThat(toAlice).hasSize(1);
        assertThat(toAlice.getFirst().title()).isEqualTo("Jornada 26: 4 puntos · 1 pleno");
        assertThat(toAlice.getFirst().body()).isEqualTo("Vas 1º en Resumen.");
        assertThat(toAlice.getFirst().url()).isEqualTo("/grupos/" + groupId);
        assertThat(messagesTo(bobEndpoint)).isEmpty();

        assertThat(roundNotifications.summarize(roundId)).isZero();
        assertThat(roundNotifications.roundsToSummarize(now)).doesNotContain(roundId);
    }

    @Test
    void sendsATestNotificationToTheUsersOwnDevices() throws Exception {
        String token = tokenFor("push-test@example.com", false);
        String endpoint = endpoint("test");
        subscribe(token, endpoint);
        clearInvocations(sender);

        mockMvc.perform(post("/api/push/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(1));

        assertThat(messagesTo(endpoint)).extracting(PushMessage::tag).containsExactly("test");
    }

    @Test
    void onlyAdminsCanSendToEveryone() throws Exception {
        String admin = tokenFor("push-broadcast-admin@example.com", true);
        String user = tokenFor("push-broadcast-user@example.com", false);
        String endpoint = endpoint("broadcast");
        subscribe(user, endpoint);
        clearInvocations(sender);
        String request = """
                { "title": "Nueva temporada", "body": "Ya puedes pronosticar la jornada 1.", "url": "/" }
                """;

        mockMvc.perform(post("/api/admin/push/broadcast")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/push/broadcast")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").isNumber());

        assertThat(messagesTo(endpoint)).extracting(PushMessage::title).containsExactly("Nueva temporada");
    }

    @Test
    void deletingTheAccountRemovesItsDevices() throws Exception {
        String token = tokenFor("push-delete@example.com", false);
        String endpoint = endpoint("delete");
        subscribe(token, endpoint);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"password\": \"password123\" }"))
                .andExpect(status().isNoContent());

        assertThat(subscriptionsFor(endpoint)).isZero();
    }

    // ---------- Ayudas ----------

    private static String endpoint(String name) {
        return "https://fcm.googleapis.com/fcm/send/" + name + "-" + System.nanoTime();
    }

    private ResultActions subscribe(String token, String endpoint) throws Exception {
        return subscribe(token, endpoint, P256DH, AUTH);
    }

    private ResultActions subscribe(String token, String endpoint, String p256dh, String auth) throws Exception {
        return mockMvc.perform(put("/api/push/subscriptions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "endpoint": "%s", "keys": { "p256dh": "%s", "auth": "%s" } }
                        """.formatted(endpoint, p256dh, auth)));
    }

    private ResultActions unsubscribe(String token, String endpoint) throws Exception {
        return mockMvc.perform(delete("/api/push/subscriptions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "endpoint": "%s" }
                        """.formatted(endpoint)));
    }

    /** Mensajes enviados a un dispositivo concreto (otros tests pueden tener dispositivos suscritos). */
    private List<PushMessage> messagesTo(String endpoint) {
        ArgumentCaptor<PushTarget> targets = ArgumentCaptor.forClass(PushTarget.class);
        ArgumentCaptor<PushMessage> messages = ArgumentCaptor.forClass(PushMessage.class);
        verify(sender, atLeast(0)).send(targets.capture(), messages.capture(), any());

        List<PushMessage> result = new ArrayList<>();
        for (int i = 0; i < targets.getAllValues().size(); i++) {
            if (targets.getAllValues().get(i).endpoint().equals(endpoint)) {
                result.add(messages.getAllValues().get(i));
            }
        }
        return result;
    }

    private Integer subscriptionsFor(String endpoint) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM push_subscription WHERE endpoint = ?", Integer.class, endpoint);
    }

    private Long ownerOf(String endpoint) {
        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM push_subscription WHERE endpoint = ?", Long.class, endpoint);
    }

    private Long userId(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE email = ?", Long.class, email);
    }
}
