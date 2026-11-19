package com.raulcedroj.football_predictions.notification.webpush;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

/**
 * Cifra el contenido de una notificación como pide Web Push (RFC 8291, codificación aes128gcm de RFC 8188).
 * El servicio de push del navegador (Google, Apple, Mozilla) no puede leer el mensaje: solo el dispositivo,
 * que tiene la clave privada de la suscripción.
 */
public final class WebPushEncryption {

    private static final int RECORD_SIZE = 4096;
    private static final int SALT_LENGTH = 16;
    private static final int AUTH_SECRET_LENGTH = 16;
    private static final int MAX_PAYLOAD = 3000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushEncryption() {
    }

    /**
     * @param uaPublic   clave pública del navegador ({@code keys.p256dh} de la suscripción)
     * @param authSecret secreto de la suscripción ({@code keys.auth})
     */
    public static byte[] encrypt(byte[] payload, byte[] uaPublic, byte[] authSecret) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return encrypt(payload, uaPublic, authSecret, EcKeys.generate(), salt);
    }

    /** Con la clave efímera y el salt fijados, para poder comprobarlo con el ejemplo del RFC. */
    static byte[] encrypt(byte[] payload, byte[] uaPublic, byte[] authSecret, KeyPair serverKeys, byte[] salt) {
        if (payload.length > MAX_PAYLOAD) {
            throw new IllegalArgumentException("Push payload too large: " + payload.length + " bytes");
        }
        if (authSecret.length != AUTH_SECRET_LENGTH) {
            throw new IllegalArgumentException("The auth secret has 16 bytes");
        }
        try {
            ECPublicKey uaKey = EcKeys.publicKey(uaPublic);
            byte[] serverPublic = EcKeys.encode((ECPublicKey) serverKeys.getPublic());

            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(serverKeys.getPrivate());
            agreement.doPhase(uaKey, true);
            byte[] ecdhSecret = agreement.generateSecret();

            byte[] keyInfo = concat(ascii("WebPush: info\0"), uaPublic, serverPublic);
            byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);
            byte[] contentKey = hkdf(salt, ikm, ascii("Content-Encoding: aes128gcm\0"), 16);
            byte[] nonce = hkdf(salt, ikm, ascii("Content-Encoding: nonce\0"), 12);

            // Un único registro: el contenido seguido del delimitador 0x02 (último registro), sin relleno
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(contentKey, "AES"), new GCMParameterSpec(128, nonce));
            byte[] ciphertext = cipher.doFinal(concat(payload, new byte[]{2}));

            byte[] header = ByteBuffer.allocate(SALT_LENGTH + 4 + 1 + serverPublic.length)
                    .put(salt)
                    .putInt(RECORD_SIZE)
                    .put((byte) serverPublic.length)
                    .put(serverPublic)
                    .array();
            return concat(header, ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt the push message", e);
        }
    }

    /** HKDF-SHA256 (RFC 5869) para salidas de hasta 32 bytes, que es todo lo que hace falta aquí. */
    static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
        byte[] prk = hmac(salt, ikm);
        return Arrays.copyOf(hmac(prk, concat(info, new byte[]{1})), length);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        for (byte[] part : parts) {
            buffer.put(part);
        }
        return buffer.array();
    }
}
