package com.raulcedroj.football_predictions.notification.webpush;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VapidKeysTest {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    @Test
    void signsATokenForTheOriginOfThePushService() throws Exception {
        KeyPair pair = EcKeys.generate();
        VapidKeys keys = VapidKeys.parse(publicKey(pair), privateKey(pair), "mailto:hola@golifyapp.com");

        String header = keys.authorizationHeader(
                URI.create("https://fcm.googleapis.com/fcm/send/abc"), Instant.parse("2026-11-18T12:00:00Z"));

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + publicKey(pair));
        String[] jwt = header.substring("vapid t=".length(), header.indexOf(',')).split("\\.");
        String claims = new String(DECODER.decode(jwt[1]), StandardCharsets.UTF_8);
        assertThat(claims).isEqualTo(
                "{\"aud\":\"https://fcm.googleapis.com\",\"exp\":1795046400,\"sub\":\"mailto:hola@golifyapp.com\"}");

        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(pair.getPublic());
        verifier.update((jwt[0] + "." + jwt[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(DECODER.decode(jwt[2]))).isTrue();
    }

    @Test
    void rejectsKeysThatAreNotAPair() {
        KeyPair one = EcKeys.generate();
        KeyPair other = EcKeys.generate();

        assertThatThrownBy(() -> VapidKeys.parse(publicKey(one), privateKey(other), "mailto:hola@golifyapp.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void requiresAContactSubject() {
        KeyPair pair = EcKeys.generate();

        assertThatThrownBy(() -> VapidKeys.parse(publicKey(pair), privateKey(pair), "hola@golifyapp.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String publicKey(KeyPair pair) {
        return ENCODER.encodeToString(EcKeys.encode((ECPublicKey) pair.getPublic()));
    }

    /** La clave privada "en crudo" son los 32 bytes del escalar. */
    private static String privateKey(KeyPair pair) {
        byte[] d = ((ECPrivateKey) pair.getPrivate()).getS().toByteArray();
        byte[] fixed = new byte[32];
        int length = Math.min(d.length, 32);
        System.arraycopy(d, d.length - length, fixed, 32 - length, length);
        return ENCODER.encodeToString(fixed);
    }
}
