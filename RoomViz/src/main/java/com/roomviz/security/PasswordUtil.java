package com.roomviz.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int ITER = 120_000;
    private static final int KEY_BITS = 256;

    // Stored format: iterations:saltBase64:hashBase64
    public static String hash(char[] password) {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);

        byte[] dk = pbkdf2(password, salt, ITER, KEY_BITS);
        return ITER + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(dk);
    }

    public static boolean verify(char[] password, String stored) {
        if (stored == null || stored.isBlank()) return false;

        String[] parts = stored.split(":");
        if (parts.length != 3) return false;

        int it = Integer.parseInt(parts[0]);
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);

        byte[] actual = pbkdf2(password, salt, it, expected.length * 8);
        return constantTimeEquals(actual, expected);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= (a[i] ^ b[i]);
        return r == 0;
    }
}