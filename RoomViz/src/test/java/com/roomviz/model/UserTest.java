package com.roomviz.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void fullConstructor_adminRole() {
        User u = new User(1, "Alice", "alice@test.com", "hash123", "Designer", "Design", "ADMIN");

        assertEquals(1, u.getId());
        assertEquals("Alice", u.getFullName());
        assertEquals("alice@test.com", u.getEmail());
        assertEquals("hash123", u.getPasswordHash());
        assertEquals("Designer", u.getJobTitle());
        assertEquals("Design", u.getDepartment());
        assertEquals("ADMIN", u.getRole());
        assertTrue(u.isAdmin());
        assertFalse(u.isCustomer());
    }

    @Test
    void fullConstructor_customerRole() {
        User u = new User(2, "Bob", "bob@test.com", "hash456", "", "", "CUSTOMER");

        assertEquals("CUSTOMER", u.getRole());
        assertFalse(u.isAdmin());
        assertTrue(u.isCustomer());
    }

    @Test
    void nullRole_defaultsToAdmin() {
        User u = new User(3, "Charlie", "charlie@test.com", "hash789", "", "", null);

        assertEquals("ADMIN", u.getRole());
        assertTrue(u.isAdmin());
    }

    @Test
    void blankRole_defaultsToAdmin() {
        User u = new User(4, "Dana", "dana@test.com", "hashABC", "", "", "   ");

        assertEquals("ADMIN", u.getRole());
        assertTrue(u.isAdmin());
    }

    @Test
    void role_isCaseInsensitive() {
        User admin = new User(5, "Eve", "eve@test.com", "h1", "", "", "admin");
        assertTrue(admin.isAdmin());

        User customer = new User(6, "Frank", "frank@test.com", "h2", "", "", "customer");
        assertTrue(customer.isCustomer());
    }

    @Test
    void twoArgConstructor_defaultsToAdmin() {
        User u = new User(7, "Grace", "grace@test.com", "hash");

        assertEquals("ADMIN", u.getRole());
        assertTrue(u.isAdmin());
        assertEquals("", u.getJobTitle());
        assertEquals("", u.getDepartment());
    }

    @Test
    void sixArgConstructor_defaultsToAdmin() {
        User u = new User(8, "Hank", "hank@test.com", "hash", "Engineer", "R&D");

        assertEquals("ADMIN", u.getRole());
        assertTrue(u.isAdmin());
        assertEquals("Engineer", u.getJobTitle());
        assertEquals("R&D", u.getDepartment());
    }
}
