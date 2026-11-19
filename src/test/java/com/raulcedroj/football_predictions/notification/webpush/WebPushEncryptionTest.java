package com.raulcedroj.football_predictions.notification.webpush;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebPushEncryptionTest {

    private static final Base64.Decoder BASE64URL = Base64.getUrlDecoder();

    /** Ejemplo completo del apéndice A de RFC 8291: con las mismas claves y salt, el resultado tiene que ser idéntico. */
    @Test
    void matchesTheRfc8291Example() {
        KeyPair serverKeys = new KeyPair(
                EcKeys.publicKey(decode("BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8")),
                EcKeys.privateKey(decode("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw")));

        byte[] encrypted = WebPushEncryption.encrypt(
                "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.UTF_8),
                decode("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4"),
                decode("BTBZMqHH6r4Tts7J_aSIgg"),
                serverKeys,
                decode("DGv6ra1nlYgDCS1FRnbzlw"));

        assertThat(Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted)).isEqualTo(
                "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");
    }

    @Test
    void usesANewKeyAndSaltForEveryMessage() {
        byte[] uaPublic = EcKeys.encode((java.security.interfaces.ECPublicKey) EcKeys.generate().getPublic());
        byte[] auth = new byte[16];
        byte[] payload = "hola".getBytes(StandardCharsets.UTF_8);

        assertThat(WebPushEncryption.encrypt(payload, uaPublic, auth))
                .isNotEqualTo(WebPushEncryption.encrypt(payload, uaPublic, auth));
    }

    @Test
    void rejectsInvalidSubscriptionKeys() {
        assertThatThrownBy(() -> WebPushEncryption.encrypt(new byte[1], new byte[65], new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WebPushEncryption.encrypt(new byte[1], new byte[10], new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] decode(String value) {
        return BASE64URL.decode(value);
    }
}
