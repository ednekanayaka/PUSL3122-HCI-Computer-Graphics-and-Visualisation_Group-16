package com.roomviz.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashAndVerify_roundTrip() {
        char[] password = "MySecretPass123!".toCharArray();
        String hashed = PasswordUtil.hash(password);

        assertNotNull(hashed);
        assertFalse(hashed.isBlank());
        assertTrue(PasswordUtil.verify("MySecretPass123!".toCharArray(), hashed));
    }

    @Test
    void verify_wrongPassword_returnsFalse() {
        char[] password = "CorrectPassword".toCharArray();
        String hashed = PasswordUtil.hash(password);

        assertFalse(PasswordUtil.verify("WrongPassword".toCharArray(), hashed));
    }

    @Test
    void hash_samePassword_differentHashes() {
        char[] password = "SamePassword".toCharArray();
        String hash1 = PasswordUtil.hash(password);
        String hash2 = PasswordUtil.hash("SamePassword".toCharArray());

        // Different random salts should produce different stored strings
        assertNotEquals(hash1, hash2);
    }

    @Test
    void verify_nullStoredHash_returnsFalse() {
        assertFalse(PasswordUtil.verify("any".toCharArray(), null));
    }

    @Test
    void verify_blankStoredHash_returnsFalse() {
        assertFalse(PasswordUtil.verify("any".toCharArray(), ""));
        assertFalse(PasswordUtil.verify("any".toCharArray(), "   "));
    }

    @Test
    void hash_format_hasThreeParts() {
        String hashed = PasswordUtil.hash("test".toCharArray());
        String[] parts = hashed.split(":");
        assertEquals(3, parts.length, "Hash format should be iterations:salt:hash");
    }

    @Test
    void verify_emptyPassword_worksCorrectly() {
        char[] empty = "".toCharArray();
        String hashed = PasswordUtil.hash(empty);

        assertTrue(PasswordUtil.verify("".toCharArray(), hashed));
        assertFalse(PasswordUtil.verify("notempty".toCharArray(), hashed));
    }
}
