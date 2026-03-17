package com.roomviz.data;

import com.roomviz.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Test
    void initialState_notLoggedIn() {
        Session session = new Session();

        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
    }

    @Test
    void login_setsUser() {
        Session session = new Session();
        User user = new User(1, "Alice", "alice@test.com", "hash");

        session.login(user);

        assertTrue(session.isLoggedIn());
        assertNotNull(session.getCurrentUser());
        assertEquals("Alice", session.getCurrentUser().getFullName());
        assertEquals(1, session.getCurrentUser().getId());
    }

    @Test
    void logout_clearsUser() {
        Session session = new Session();
        User user = new User(1, "Bob", "bob@test.com", "hash");

        session.login(user);
        assertTrue(session.isLoggedIn());

        session.logout();

        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
    }

    @Test
    void login_replacesExistingUser() {
        Session session = new Session();
        User user1 = new User(1, "Alice", "alice@test.com", "hash1");
        User user2 = new User(2, "Bob", "bob@test.com", "hash2");

        session.login(user1);
        assertEquals("Alice", session.getCurrentUser().getFullName());

        session.login(user2);
        assertEquals("Bob", session.getCurrentUser().getFullName());
        assertEquals(2, session.getCurrentUser().getId());
    }
}
