package com.raulcedroj.football_predictions.notification.webpush;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Identifica al servidor ante los servicios de push (RFC 8292). Cada envío lleva un JWT firmado con la
 * clave privada VAPID; el navegador se suscribió con la pública, así que solo este servidor puede
 * mandar notificaciones a esas suscripciones.
 */
public final class VapidKeys {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(12);
    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

    private final ECPrivateKey privateKey;
    private final ECPublicKey publicKey;
    private final String publicKeyBase64;
    private final String subject;

    private VapidKeys(ECPrivateKey privateKey, ECPublicKey publicKey, String subject) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.publicKeyBase64 = BASE64URL.encodeToString(EcKeys.encode(publicKey));
        this.subject = subject;
    }

    /**
     * @param publicKey  clave pública en base64url (65 bytes), como la genera {@code npx web-push generate-vapid-keys}
     * @param privateKey clave privada en base64url (32 bytes)
     * @param subject    contacto para el servicio de push: {@code mailto:...} o {@code https://...}
     */
    public static VapidKeys parse(String publicKey, String privateKey, String subject) {
        if (!subject.startsWith("mailto:") && !subject.startsWith("https://")) {
            throw new IllegalArgumentException("The VAPID subject must be a mailto: or https: URL");
        }
        Base64.Decoder decoder = Base64.getUrlDecoder();
        VapidKeys keys = new VapidKeys(
                EcKeys.privateKey(decoder.decode(privateKey.strip())),
                EcKeys.publicKey(decoder.decode(publicKey.strip())),
                subject);
        keys.checkPair();
        return keys;
    }

    static VapidKeys of(KeyPair pair, String subject) {
        return new VapidKeys((ECPrivateKey) pair.getPrivate(), (ECPublicKey) pair.getPublic(), subject);
    }

    public String publicKey() {
        return publicKeyBase64;
    }

    /** Cabecera {@code Authorization} para enviar a {@code endpoint}. */
    public String authorizationHeader(URI endpoint, Instant now) {
        String audience = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
        String header = encodeJson("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
        String claims = encodeJson("{\"aud\":\"%s\",\"exp\":%d,\"sub\":\"%s\"}"
                .formatted(audience, now.plus(TOKEN_VALIDITY).getEpochSecond(), subject));
        String unsigned = header + "." + claims;
        return "vapid t=" + unsigned + "." + BASE64URL.encodeToString(sign(unsigned)) + ", k=" + publicKeyBase64;
    }

    /** ES256 en JWT es la firma en formato R||S (64 bytes), no en DER. */
    private byte[] sign(String data) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.US_ASCII));
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign the VAPID token", e);
        }
    }

    /** Falla al arrancar si las dos claves no son pareja, en vez de fallar en cada envío. */
    private void checkPair() {
        try {
            byte[] probe = "vapid-key-check".getBytes(StandardCharsets.US_ASCII);
            Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
            signer.initSign(privateKey);
            signer.update(probe);
            byte[] signed = signer.sign();

            Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
            verifier.initVerify(publicKey);
            verifier.update(probe);
            if (!verifier.verify(signed)) {
                throw new IllegalArgumentException("The VAPID public and private keys do not match");
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encodeJson(String json) {
        return BASE64URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
