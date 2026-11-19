package com.raulcedroj.football_predictions.notification.webpush;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

/** Claves de curva elíptica P-256 en el formato "en crudo" que usa Web Push (sin ASN.1). */
final class EcKeys {

    static final int PUBLIC_KEY_LENGTH = 65;
    private static final int COORDINATE_LENGTH = 32;
    private static final ECParameterSpec P256 = p256();

    private EcKeys() {
    }

    static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Punto sin comprimir: 0x04 + X (32 bytes) + Y (32 bytes). */
    static ECPublicKey publicKey(byte[] uncompressed) {
        if (uncompressed.length != PUBLIC_KEY_LENGTH || uncompressed[0] != 0x04) {
            throw new IllegalArgumentException("Not an uncompressed P-256 public key");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 1 + COORDINATE_LENGTH));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1 + COORDINATE_LENGTH, PUBLIC_KEY_LENGTH));
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC")
                    .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid P-256 public key", e);
        }
    }

    static ECPrivateKey privateKey(byte[] d) {
        if (d.length != COORDINATE_LENGTH) {
            throw new IllegalArgumentException("A P-256 private key has 32 bytes");
        }
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, d), P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid P-256 private key", e);
        }
    }

    static byte[] encode(ECPublicKey key) {
        byte[] out = new byte[PUBLIC_KEY_LENGTH];
        out[0] = 0x04;
        copyFixed(key.getW().getAffineX(), out, 1);
        copyFixed(key.getW().getAffineY(), out, 1 + COORDINATE_LENGTH);
        return out;
    }

    /** BigInteger puede traer un 0 de signo delante o venir más corto: se ajusta a 32 bytes. */
    private static void copyFixed(BigInteger value, byte[] out, int offset) {
        byte[] bytes = value.toByteArray();
        int length = Math.min(bytes.length, COORDINATE_LENGTH);
        System.arraycopy(bytes, bytes.length - length, out, offset + COORDINATE_LENGTH - length, length);
    }

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
